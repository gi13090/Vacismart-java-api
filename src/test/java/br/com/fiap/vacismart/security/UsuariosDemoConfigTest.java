package br.com.fiap.vacismart.security;

import br.com.fiap.vacismart.config.UsuariosDemoConfig;
import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.Perfil;
import br.com.fiap.vacismart.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.DefaultApplicationArguments;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class UsuariosDemoConfigTest {
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    UnidadeSaudeRepository unidades = mock(UnidadeSaudeRepository.class);
    PasswordEncoder senhas = mock(PasswordEncoder.class);
    MockEnvironment env = new MockEnvironment();
    UsuariosDemoConfig bootstrap = new UsuariosDemoConfig(env,usuarios,unidades,senhas);
    DefaultApplicationArguments args = new DefaultApplicationArguments();
    void credenciais() {
        env.setActiveProfiles("dev");
        env.setProperty("VACISMART_DEMO_CONFIRMACAO","DADOS_FICTICIOS_LOCAIS");
        for(String papel: new String[]{"CIDADAO","FUNCIONARIO_A","FUNCIONARIO_B","GESTOR"}) {
            env.setProperty("DEMO_"+papel+"_EMAIL",papel.toLowerCase()+"@teste.invalid");
            env.setProperty("DEMO_"+papel+"_SENHA","FicticiaSomenteTeste!123");
        }
    }
    @Test void criaQuatroPerfisEDuasUnidadesComHash() {
        credenciais();
        when(unidades.save(any())).thenAnswer(i -> i.getArgument(0));
        when(senhas.encode(any())).thenReturn("hash-ficticio");
        bootstrap.run(args);
        var captor=org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios,times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Usuario::getPerfil)
            .containsExactly(Perfil.CIDADAO,Perfil.FUNCIONARIO,Perfil.FUNCIONARIO,Perfil.GESTOR);
        assertThat(captor.getAllValues().get(1).getUnidadeSaude()).isNotSameAs(captor.getAllValues().get(2).getUnidadeSaude());
        verify(senhas,times(4)).encode(any()); verify(unidades,times(2)).save(any());
    }
    @ParameterizedTest @ValueSource(strings={"prod","dev,prod","dev,outro","test",""})
    void recusaAmbienteDiferenteDeDevExclusivo(String perfis) {
        credenciais();env.setActiveProfiles(perfis.isEmpty()?new String[0]:perfis.split(","));
        assertThatIllegalStateException().isThrownBy(() -> bootstrap.run(args));
        verifyNoInteractions(usuarios,unidades,senhas);
    }
    @Test void exigeConfirmacaoERejeitaCredenciaisAusentesAntesDePersistir() {
        env.setActiveProfiles("dev");
        assertThatIllegalStateException().isThrownBy(() -> bootstrap.run(args));
        env.setProperty("VACISMART_DEMO_CONFIRMACAO","DADOS_FICTICIOS_LOCAIS");
        assertThatIllegalStateException().isThrownBy(() -> bootstrap.run(args));
        verifyNoInteractions(usuarios,unidades,senhas);
    }
    @Test void naoSobrescreveContaExistente() {
        credenciais();when(usuarios.existsByEmail(any())).thenReturn(true);
        assertThatIllegalStateException().isThrownBy(() -> bootstrap.run(args));
        verify(usuarios,never()).save(any());verifyNoInteractions(unidades,senhas);
    }
    @Test void funcionarioExigeUnidadeEOutrosPerfisNaoAceitamVinculo() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Usuario("Nome","a@teste.invalid","hash",Perfil.FUNCIONARIO,null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Usuario("Nome","a@teste.invalid","hash",Perfil.GESTOR,new UnidadeSaude("A")));
        assertThatIllegalArgumentException().isThrownBy(() -> new Usuario("Nome","a@teste.invalid","hash",Perfil.CIDADAO,new UnidadeSaude("A")));
    }
}
