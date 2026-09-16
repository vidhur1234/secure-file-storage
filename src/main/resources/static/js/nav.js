// Navigation Bar Renderer

document.addEventListener('DOMContentLoaded', () => {
    renderNavbar();
    renderFooter();
});

function renderNavbar() {
    const navPlaceholder = document.getElementById('navbar-placeholder');
    if (!navPlaceholder) return;

    const user = getCurrentUser();
    const currentPath = window.location.pathname;

    let navLinks = '';

    if (user) {
        const roles = user.roles || [];
        const isAdm = roles.includes('ROLE_ADMIN');
        const isMgr = roles.includes('ROLE_MANAGER');
        const isAud = roles.includes('ROLE_AUDITOR');
        const isEmp = roles.includes('ROLE_EMPLOYEE');

        // Dashboard (available to all authenticated users)
        navLinks += `<li class="nav-item"><a class="nav-link ${currentPath.includes('admin-dashboard') ? 'active' : ''}" href="/admin-dashboard.html"><i class="fas fa-chart-line me-1"></i> Dashboard</a></li>`;

        // Users & Roles (ADMIN only)
        if (isAdm) {
            navLinks += `<li class="nav-item"><a class="nav-link ${currentPath.includes('users') ? 'active' : ''}" href="/users.html"><i class="fas fa-users-cog me-1"></i> Users</a></li>`;
            navLinks += `<li class="nav-item"><a class="nav-link ${currentPath.includes('roles') ? 'active' : ''}" href="/roles.html"><i class="fas fa-sitemap me-1"></i> Roles</a></li>`;
        }

        // Upload (ADMIN, MANAGER, EMPLOYEE)
        if (!isAud) {
            navLinks += `<li class="nav-item"><a class="nav-link ${currentPath.includes('upload') ? 'active' : ''}" href="/upload.html"><i class="fas fa-cloud-upload-alt me-1"></i> Upload</a></li>`;
        }

        // Files (All roles)
        navLinks += `<li class="nav-item"><a class="nav-link ${currentPath.includes('files') ? 'active' : ''}" href="/files.html"><i class="fas fa-folder-open me-1"></i> Files</a></li>`;

        // Audit Logs (ADMIN, AUDITOR)
        if (isAdm || isAud) {
            navLinks += `<li class="nav-item"><a class="nav-link ${currentPath.includes('audit-logs') ? 'active' : ''}" href="/audit-logs.html"><i class="fas fa-shield-alt me-1"></i> Audit Logs</a></li>`;
        }

        // Profile (All roles)
        navLinks += `<li class="nav-item"><a class="nav-link ${currentPath.includes('profile') ? 'active' : ''}" href="/profile.html"><i class="fas fa-user-shield me-1"></i> Profile</a></li>`;
    }

    const primaryRole = user && user.roles && user.roles.length > 0 ? user.roles[0].replace('ROLE_', '') : 'GUEST';
    let roleBadgeClass = 'bg-secondary';
    if (primaryRole === 'ADMIN') roleBadgeClass = 'bg-danger';
    else if (primaryRole === 'MANAGER') roleBadgeClass = 'bg-warning text-dark';
    else if (primaryRole === 'AUDITOR') roleBadgeClass = 'bg-info text-dark';
    else if (primaryRole === 'EMPLOYEE') roleBadgeClass = 'bg-primary';

    const userSection = user ? `
        <div class="d-flex align-items-center">
            <span class="text-light me-3">
                <i class="fas fa-user-circle me-1"></i> <strong>${user.username}</strong>
                <span class="badge ${roleBadgeClass} ms-1">${primaryRole}</span>
            </span>
            <button class="btn btn-outline-light btn-sm" onclick="logout()">
                <i class="fas fa-sign-out-alt me-1"></i> Logout
            </button>
        </div>
    ` : `
        <div>
            <a href="/login.html" class="btn btn-primary btn-sm"><i class="fas fa-sign-in-alt me-1"></i> Login</a>
        </div>
    `;

    navPlaceholder.innerHTML = `
        <nav class="navbar navbar-expand-lg navbar-dark navbar-cyber">
            <div class="container-fluid px-4">
                <a class="navbar-brand" href="${user ? '/admin-dashboard.html' : '/login.html'}">
                    <i class="fas fa-lock"></i> SecureStorage <span class="badge bg-cyan text-dark" style="background-color: #38bdf8; font-size: 0.7rem;">AES-256</span>
                </a>
                <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#cyberNav">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="cyberNav">
                    <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                        ${navLinks}
                    </ul>
                    ${userSection}
                </div>
            </div>
        </nav>
    `;
}

function renderFooter() {
    const footerPlaceholder = document.getElementById('footer-placeholder');
    if (!footerPlaceholder) return;

    footerPlaceholder.innerHTML = `
        <footer>
            <div class="container text-center">
                <div class="row align-items-center">
                    <div class="col-md-6 text-md-start mb-2 mb-md-0">
                        <strong>Secure File Storage System</strong> &bull; AES-256-GCM Encryption &bull; SHA-256 Integrity Verification
                    </div>
                    <div class="col-md-6 text-md-end">
                        <span class="badge bg-dark border border-secondary text-light">RBAC: Role Hierarchy Inherited</span>
                        <span class="badge bg-dark border border-secondary text-light ms-1">Prototype Demo</span>
                    </div>
                </div>
            </div>
        </footer>
    `;
}
