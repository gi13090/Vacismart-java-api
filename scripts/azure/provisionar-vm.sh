#!/usr/bin/env bash
# Reprodução futura da infraestrutura informada. Não instala Docker nem faz deploy.
set -euo pipefail
set +x
umask 077

usage() {
  cat <<'HELP'
Uso: bash scripts/azure/provisionar-vm.sh \
  --subscription ID --admin-user USUARIO --ssh-public-key ARQUIVO.pub \
  --ssh-source-cidr IPv4/32 --ssh-rule-name NOME --ssh-priority NUMERO \
  --vnet-cidr CIDR --subnet-cidr CIDR [--apply]

Sem --apply: valida entradas e mostra o plano, sem chamar Azure CLI.
Com --apply: exige terminal e confirmação digitada antes de consultar/provisionar.
Destino: rg-vacismart, spaincentral, vm-vacismart, Standard_D2s_v4,
Ubuntu 22.04, disco StandardSSD_LRS de 64 GB, autenticação ED25519.
Não gera/sobrescreve chaves, não abre 8080/5432 e não exclui recursos.
Recursos existentes compatíveis são reutilizados; divergências abortam a operação.
HELP
}
die() { printf 'ERRO: %s\n' "$*" >&2; exit 1; }

VS_SUBSCRIPTION='' VS_ADMIN='' VS_PUBLIC_KEY='' VS_SSH_CIDR=''
VS_SSH_RULE='' VS_SSH_PRIORITY='' VS_VNET_CIDR='' VS_SUBNET_CIDR=''
VS_APPLY=false
while (($#)); do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --apply) VS_APPLY=true; shift ;;
    --subscription|--admin-user|--ssh-public-key|--ssh-source-cidr|--ssh-rule-name|--ssh-priority|--vnet-cidr|--subnet-cidr)
      (($# >= 2)) || die "Valor ausente para $1"
      [[ $2 != --* && -n $2 ]] || die "Valor inválido para $1"
      case "$1" in
        --subscription) VS_SUBSCRIPTION=$2 ;;
        --admin-user) VS_ADMIN=$2 ;;
        --ssh-public-key) VS_PUBLIC_KEY=$2 ;;
        --ssh-source-cidr) VS_SSH_CIDR=$2 ;;
        --ssh-rule-name) VS_SSH_RULE=$2 ;;
        --ssh-priority) VS_SSH_PRIORITY=$2 ;;
        --vnet-cidr) VS_VNET_CIDR=$2 ;;
        --subnet-cidr) VS_SUBNET_CIDR=$2 ;;
      esac
      shift 2 ;;
    *) die "Parâmetro desconhecido: $1" ;;
  esac
done
for VS_REQUIRED in VS_SUBSCRIPTION VS_ADMIN VS_PUBLIC_KEY VS_SSH_CIDR VS_SSH_RULE VS_SSH_PRIORITY VS_VNET_CIDR VS_SUBNET_CIDR; do
  [[ -n ${!VS_REQUIRED} ]] || { usage >&2; die "Parâmetro obrigatório ausente: $VS_REQUIRED"; }
done
command -v python3 >/dev/null || die 'Instale Python 3 para validar as entradas.'
command -v ssh-keygen >/dev/null || die 'Instale o cliente OpenSSH.'
[[ $VS_PUBLIC_KEY == *.pub && -f $VS_PUBLIC_KEY && -r $VS_PUBLIC_KEY ]] || die 'Informe somente um arquivo de chave pública .pub legível.'
ssh-keygen -lf "$VS_PUBLIC_KEY" >/dev/null || die 'Chave pública inválida.'

readonly VS_RG=rg-vacismart VS_LOCATION=spaincentral VS_VM=vm-vacismart
readonly VS_NSG=nsg-vacismart VS_VNET=vnet-vacismart VS_SUBNET=subnet-vacismart
readonly VS_PIP=pip-vacismart VS_NIC=nic-vacismart
readonly VS_SIZE=Standard_D2s_v4 VS_DISK_SKU=StandardSSD_LRS VS_DISK_GB=64
# Alias da família 22.04: o patch disponível pode diferir do 22.04.5 observado.
readonly VS_IMAGE=Ubuntu2204
export VS_SUBSCRIPTION VS_ADMIN VS_PUBLIC_KEY VS_SSH_CIDR VS_SSH_RULE VS_SSH_PRIORITY
export VS_VNET_CIDR VS_SUBNET_CIDR VS_RG VS_LOCATION VS_VM VS_NSG VS_VNET VS_SUBNET
export VS_PIP VS_NIC VS_SIZE VS_DISK_SKU VS_DISK_GB

python3 - <<'PY'
import ipaddress, os, re, uuid
from pathlib import Path
def require(ok, message):
    if not ok:
        raise SystemExit('ERRO: '+message)
try:
    uuid.UUID(os.environ['VS_SUBSCRIPTION'])
    ssh=ipaddress.ip_network(os.environ['VS_SSH_CIDR'], strict=True)
    vnet=ipaddress.ip_network(os.environ['VS_VNET_CIDR'], strict=True)
    subnet=ipaddress.ip_network(os.environ['VS_SUBNET_CIDR'], strict=True)
    priority=int(os.environ['VS_SSH_PRIORITY'])
except ValueError:
    raise SystemExit('ERRO: assinatura, CIDR ou prioridade inválidos; valores omitidos.')
require(ssh.version==4 and ssh.prefixlen==32 and ssh.network_address.is_global and not ssh.network_address.is_multicast,
        'SSH exige um IPv4 público individual em /32; não use origem aberta.')
private=[ipaddress.ip_network(n) for n in ('10.0.0.0/8','172.16.0.0/12','192.168.0.0/16')]
require(vnet.version==4 and subnet.version==4 and any(vnet.subnet_of(n) for n in private),
        'VNet deve usar um CIDR IPv4 privado RFC1918.')
require(subnet.subnet_of(vnet) and 8<=vnet.prefixlen<=subnet.prefixlen<=29,
        'Sub-rede deve caber na VNet e respeitar os prefixos /8 a /29.')
require(100<=priority<=4096, 'Prioridade SSH deve estar entre 100 e 4096.')
require(bool(re.fullmatch(r'[a-z_][a-z0-9_-]{0,31}',os.environ['VS_ADMIN'])) and
        os.environ['VS_ADMIN'] not in {'root','admin','administrator'}, 'Usuário administrativo inválido.')
require(bool(re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.-]{0,79}',os.environ['VS_SSH_RULE'])),
        'Nome da regra SSH inválido.')
lines=[l for l in Path(os.environ['VS_PUBLIC_KEY']).read_text().splitlines() if l.strip()]
require(len(lines)==1 and len(lines[0].split())>=2 and lines[0].split()[0]=='ssh-ed25519',
        'Informe exatamente uma chave pública ED25519, nunca a chave privada.')
PY

printf '%s\n' 'Plano: rg-vacismart/spaincentral; NSG SSH TCP/22 restrito à origem /32 informada.'
printf '%s\n' 'VNet/sub-rede, IP Standard estático IPv4, NIC e VM Ubuntu 22.04 Standard_D2s_v4.'
printf '%s\n' 'Disco StandardSSD_LRS 64 GB; nenhuma porta pública de API/banco será criada.'
if [[ $VS_APPLY != true ]]; then
  printf '%s\n' 'Somente plano local. Nenhuma chamada Azure. Use --apply e confirme para uma execução futura.'
  exit 0
fi
[[ -t 0 ]] || die 'Provisionamento exige confirmação em terminal interativo.'
read -r -p 'Para prosseguir, digite exatamente PROVISIONAR rg-vacismart: ' VS_CONFIRM
[[ $VS_CONFIRM == 'PROVISIONAR rg-vacismart' ]] || die 'Confirmação não recebida; nada provisionado.'
command -v az >/dev/null || die 'Instale Azure CLI e autentique-se previamente.'
azc() { az "$@" --subscription "$VS_SUBSCRIPTION" --only-show-errors; }
azc account show --output none

# Não converter erros de autenticação/rede/permissão em "recurso ausente".
VS_GROUP_EXISTS=$(azc group exists --name "$VS_RG" --output tsv)
[[ $VS_GROUP_EXISTS == true || $VS_GROUP_EXISTS == false ]] || die 'Não foi possível determinar se o grupo existe.'
VS_RG_JSON=null VS_NSG_JSON=null VS_VNET_JSON=null VS_SUBNET_JSON=null
VS_PIP_JSON=null VS_NIC_JSON=null VS_VM_JSON=null VS_DISK_JSON=null
lookup() {
  local resource_type=$1 resource_name=$2 count
  shift 2
  count=$(azc resource list --resource-group "$VS_RG" --resource-type "$resource_type" \
    --query "[?name=='$resource_name'] | length(@)" --output tsv) || die 'Falha ao consultar inventário Azure.'
  case "$count" in
    0) printf 'null\n' ;;
    1) azc "$@" --resource-group "$VS_RG" --name "$resource_name" --output json || die 'Falha ao inspecionar recurso existente.' ;;
    *) die 'Inventário inesperado; revisar sem alterar recursos.' ;;
  esac
}
if [[ $VS_GROUP_EXISTS == true ]]; then
  VS_RG_JSON=$(azc group show --name "$VS_RG" --output json)
  VS_NSG_JSON=$(lookup Microsoft.Network/networkSecurityGroups "$VS_NSG" network nsg show)
  VS_VNET_JSON=$(lookup Microsoft.Network/virtualNetworks "$VS_VNET" network vnet show)
  VS_PIP_JSON=$(lookup Microsoft.Network/publicIPAddresses "$VS_PIP" network public-ip show)
  VS_NIC_JSON=$(lookup Microsoft.Network/networkInterfaces "$VS_NIC" network nic show)
  VS_VM_JSON=$(lookup Microsoft.Compute/virtualMachines "$VS_VM" vm show)
  if [[ $VS_VNET_JSON != null ]]; then
    VS_SUBNET_JSON=$(azc network vnet subnet list --resource-group "$VS_RG" --vnet-name "$VS_VNET" \
      --query "[?name=='$VS_SUBNET'] | [0]" --output json)
    VS_SUBNET_JSON=${VS_SUBNET_JSON:-null}
  fi
  if [[ $VS_VM_JSON != null ]]; then
    VS_DISK_ID=$(azc vm show --resource-group "$VS_RG" --name "$VS_VM" --query storageProfile.osDisk.managedDisk.id --output tsv)
    [[ -n $VS_DISK_ID ]] || die 'VM existente sem disco gerenciado identificável.'
    VS_DISK_JSON=$(azc disk show --ids "$VS_DISK_ID" --output json)
  fi
fi
export VS_RG_JSON VS_NSG_JSON VS_VNET_JSON VS_SUBNET_JSON VS_PIP_JSON VS_NIC_JSON VS_VM_JSON VS_DISK_JSON

# Validar TODOS os recursos existentes antes da primeira criação. Divergência não é corrigida automaticamente.
VS_SSH_EXISTS=$(python3 - <<'PY'
import json, os
from pathlib import Path
e=os.environ
def get(k): return json.loads(e['VS_'+k+'_JSON'])
def check(ok,message):
    if not ok: raise SystemExit('ERRO: '+message+' Nenhum recurso existente será sobrescrito.')
def same(a,b): return str(a or '').lower()==str(b or '').lower()
prefix=f"/subscriptions/{e['VS_SUBSCRIPTION']}/resourceGroups/{e['VS_RG']}/providers/"
nsg_id=prefix+'Microsoft.Network/networkSecurityGroups/'+e['VS_NSG']
subnet_id=prefix+'Microsoft.Network/virtualNetworks/'+e['VS_VNET']+'/subnets/'+e['VS_SUBNET']
pip_id=prefix+'Microsoft.Network/publicIPAddresses/'+e['VS_PIP']
nic_id=prefix+'Microsoft.Network/networkInterfaces/'+e['VS_NIC']
vm_id=prefix+'Microsoft.Compute/virtualMachines/'+e['VS_VM']
rg,nsg,vnet,subnet,pip,nic,vm,disk=(get(k) for k in ['RG','NSG','VNET','SUBNET','PIP','NIC','VM','DISK'])
for obj in [rg,nsg,vnet,pip,nic,vm]:
    if obj: check(same(obj.get('location'),e['VS_LOCATION']), 'Região de recurso existente divergente.')
ssh_rule=None
if nsg:
    for rule in nsg.get('securityRules',[]):
        if rule['name']==e['VS_SSH_RULE']: ssh_rule=rule
        elif rule.get('priority')==int(e['VS_SSH_PRIORITY']): check(False,'Prioridade SSH já usada por outra regra.')
        if rule.get('direction')=='Inbound' and rule.get('access')=='Allow':
            check(rule['name']==e['VS_SSH_RULE'],'NSG contém outra liberação de entrada; revisar firewall.')
    if ssh_rule:
        sources=ssh_rule.get('sourceAddressPrefixes') or [ssh_rule.get('sourceAddressPrefix')]
        ports=ssh_rule.get('destinationPortRanges') or [ssh_rule.get('destinationPortRange')]
        check(ssh_rule.get('direction')=='Inbound' and ssh_rule.get('access')=='Allow' and
              ssh_rule.get('protocol','').lower()=='tcp' and sources==[e['VS_SSH_CIDR']] and
              ports==['22'] and ssh_rule.get('priority')==int(e['VS_SSH_PRIORITY']),
              'Regra SSH existente não corresponde aos parâmetros explícitos.')
if vnet: check(vnet['addressSpace']['addressPrefixes']==[e['VS_VNET_CIDR']],'CIDR da VNet divergente.')
if subnet:
    check((subnet.get('addressPrefixes') or [subnet.get('addressPrefix')])==[e['VS_SUBNET_CIDR']], 'CIDR da sub-rede divergente.')
    subnet_nsg=subnet.get('networkSecurityGroup') or {}
    check(not subnet_nsg or same(subnet_nsg.get('id'),nsg_id),'Sub-rede ligada a outro NSG.')
if pip:
    check(pip.get('sku',{}).get('name')=='Standard' and pip.get('publicIPAllocationMethod')=='Static'
          and pip.get('publicIPAddressVersion')=='IPv4','IP existente deve ser Standard/Static/IPv4.')
    bound=(pip.get('ipConfiguration') or {}).get('id','')
    check(not bound or bound.lower().startswith(nic_id.lower()+'/ipconfigurations/'),'IP ligado a outra NIC.')
if nic:
    configs=nic.get('ipConfigurations',[])
    check(len(configs)==1 and same(configs[0].get('subnet',{}).get('id'),subnet_id)
          and same((configs[0].get('publicIPAddress') or {}).get('id'),pip_id)
          and same((nic.get('networkSecurityGroup') or {}).get('id'),nsg_id), 'NIC/rede/IP/NSG divergentes.')
    bound=(nic.get('virtualMachine') or {}).get('id')
    check(not bound or same(bound,vm_id),'NIC ligada a outra VM.')
if vm:
    check(all([nsg,vnet,subnet,pip,nic,ssh_rule,disk]),'VM existente com dependências não correspondentes.')
    check(vm['hardwareProfile']['vmSize']==e['VS_SIZE'],'Tamanho da VM divergente.')
    check(vm['osProfile']['adminUsername']==e['VS_ADMIN'],'Usuário administrativo divergente.')
    linux=vm['osProfile'].get('linuxConfiguration') or {}
    check(linux.get('disablePasswordAuthentication') is True,'Autenticação por senha não está desabilitada.')
    wanted=' '.join(Path(e['VS_PUBLIC_KEY']).read_text().split()[:2])
    installed=[' '.join(k.get('keyData','').split()[:2]) for k in linux.get('ssh',{}).get('publicKeys',[])]
    check(wanted in installed,'Chave pública informada difere da VM existente.')
    interfaces=vm['networkProfile']['networkInterfaces']
    check(len(interfaces)==1 and same(interfaces[0]['id'],nic_id),'NIC da VM divergente.')
    image=vm['storageProfile'].get('imageReference') or {}
    check(image.get('publisher','').lower()=='canonical' and 'jammy' in image.get('offer','').lower()
          and '22_04' in image.get('sku',''),'VM existente não corresponde à família Ubuntu 22.04 esperada.')
    check(disk['sku']['name']==e['VS_DISK_SKU'] and disk['diskSizeGb']==int(e['VS_DISK_GB']),
          'SKU/tamanho do disco existente divergente.')
print('true' if ssh_rule else 'false')
PY
)

if [[ $VS_GROUP_EXISTS == false ]]; then
  azc group create --name "$VS_RG" --location "$VS_LOCATION" --output none
fi
if [[ $VS_NSG_JSON == null ]]; then
  azc network nsg create --resource-group "$VS_RG" --name "$VS_NSG" --location "$VS_LOCATION" --output none
fi
if [[ $VS_VNET_JSON == null ]]; then
  azc network vnet create --resource-group "$VS_RG" --name "$VS_VNET" --location "$VS_LOCATION" \
    --address-prefixes "$VS_VNET_CIDR" --output none
fi
if [[ $VS_SUBNET_JSON == null ]]; then
  azc network vnet subnet create --resource-group "$VS_RG" --vnet-name "$VS_VNET" --name "$VS_SUBNET" \
    --address-prefixes "$VS_SUBNET_CIDR" --output none
fi
if [[ $VS_SSH_EXISTS != true ]]; then
  azc network nsg rule create --resource-group "$VS_RG" --nsg-name "$VS_NSG" --name "$VS_SSH_RULE" \
    --priority "$VS_SSH_PRIORITY" --direction Inbound --access Allow --protocol Tcp \
    --source-address-prefixes "$VS_SSH_CIDR" --source-port-ranges '*' \
    --destination-address-prefixes '*' --destination-port-ranges 22 --output none
fi
if [[ $VS_PIP_JSON == null ]]; then
  azc network public-ip create --resource-group "$VS_RG" --name "$VS_PIP" --location "$VS_LOCATION" \
    --sku Standard --allocation-method Static --version IPv4 --output none
fi
if [[ $VS_NIC_JSON == null ]]; then
  azc network nic create --resource-group "$VS_RG" --name "$VS_NIC" --location "$VS_LOCATION" \
    --vnet-name "$VS_VNET" --subnet "$VS_SUBNET" --network-security-group "$VS_NSG" \
    --public-ip-address "$VS_PIP" --output none
fi
if [[ $VS_VM_JSON == null ]]; then
  VS_NIC_ID=$(azc network nic show --resource-group "$VS_RG" --name "$VS_NIC" --query id --output tsv)
  azc vm create --resource-group "$VS_RG" --name "$VS_VM" --location "$VS_LOCATION" \
    --nics "$VS_NIC_ID" --image "$VS_IMAGE" --size "$VS_SIZE" \
    --admin-username "$VS_ADMIN" --authentication-type ssh --ssh-key-values "$VS_PUBLIC_KEY" \
    --storage-sku "$VS_DISK_SKU" --os-disk-size-gb "$VS_DISK_GB" --output none
  printf '%s\n' 'VM criada. Instalação do Docker e deploy são etapas separadas do README_DEVOPS.md.'
else
  printf '%s\n' 'VM existente compatível: reutilizada, sem recriação ou alteração.'
fi
printf '%s\n' 'Concluído. Recursos existentes preservados; nenhuma API/banco foi publicada na internet.'
