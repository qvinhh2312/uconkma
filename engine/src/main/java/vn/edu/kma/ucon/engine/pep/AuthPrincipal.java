package vn.edu.kma.ucon.engine.pep;

import vn.edu.kma.ucon.engine.pip.entity.AccountRole;

public record AuthPrincipal(
        String username,
        String displayName,
        AccountRole role,
        String studentId) {

    public boolean isAdmin() {
        return role == AccountRole.ADMIN;
    }

    public boolean isStudent() {
        return role == AccountRole.STUDENT;
    }
}
