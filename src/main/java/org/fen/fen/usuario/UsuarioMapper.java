package org.fen.fen.usuario;

import org.fen.fen.usuario.dto.UsuarioRegisterRequest;
import org.fen.fen.usuario.dto.UsuarioRegisterResponse;
import org.fen.fen.usuario.dto.SupervisorResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteDetailResponse;
import org.fen.fen.usuario.dto.UsuarioPendenteSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toUsuario(UsuarioRegisterRequest request, String normalizedEmail, String passwordHash) {
        Usuario usuario = new Usuario();
        usuario.setEmail(normalizedEmail);
        usuario.setPasswordHash(passwordHash);
        usuario.setRole(request.role());
        usuario.setSituacao(SituacaoUsuario.PENDENTE);
        return usuario;
    }

    public Funcionario toFuncionario(
            UsuarioRegisterRequest request,
            Usuario usuario,
            Funcionario supervisor
    ) {
        Funcionario funcionario = new Funcionario();
        funcionario.setUsuario(usuario);
        funcionario.setNome(request.nome().trim());
        funcionario.setCpf(request.cpf().replaceAll("\\D", ""));
        funcionario.setDataNascimento(request.dataNascimento());
        if (request.role() == Role.FARMACEUTICO) {
            funcionario.setCrf(request.crf().trim());
            funcionario.setResponsavelTecnico(request.responsavelTecnico());
        } else {
            funcionario.setTipoEstagio(request.tipoEstagio());
            funcionario.setSupervisor(supervisor);
            funcionario.setInicioVigencia(request.inicioVigencia());
            funcionario.setFimVigencia(request.fimVigencia());
        }
        return funcionario;
    }

    public UsuarioRegisterResponse toRegisterResponse(Usuario usuario, Funcionario funcionario) {
        return new UsuarioRegisterResponse(
                usuario.getId(),
                funcionario.getId(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getSituacao()
        );
    }

    public SupervisorResponse toSupervisorResponse(Funcionario funcionario) {
        return new SupervisorResponse(funcionario.getId(), funcionario.getNome());
    }

    public UsuarioPendenteSummaryResponse toPendenteSummary(
            Usuario usuario,
            Funcionario funcionario
    ) {
        return new UsuarioPendenteSummaryResponse(
                usuario.getId(),
                funcionario.getNome(),
                usuario.getEmail(),
                funcionario.getCpf(),
                usuario.getRole(),
                usuario.getCreatedAt()
        );
    }

    public UsuarioPendenteDetailResponse toPendenteDetail(
            Usuario usuario,
            Funcionario funcionario
    ) {
        SupervisorResponse supervisor = funcionario.getSupervisor() == null
                ? null
                : toSupervisorResponse(funcionario.getSupervisor());
        return new UsuarioPendenteDetailResponse(
                usuario.getId(),
                funcionario.getId(),
                funcionario.getNome(),
                usuario.getEmail(),
                funcionario.getCpf(),
                funcionario.getDataNascimento(),
                usuario.getRole(),
                usuario.getSituacao(),
                usuario.getCreatedAt(),
                funcionario.getCrf(),
                funcionario.getResponsavelTecnico(),
                funcionario.getTipoEstagio(),
                supervisor,
                funcionario.getInicioVigencia(),
                funcionario.getFimVigencia()
        );
    }
}
