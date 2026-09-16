// Secure File Storage - Authentication & API Service

const TOKEN_KEY = 'sfs_jwt_token';
const USER_KEY = 'sfs_user_info';

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function setAuth(token, userInfo) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
}

function getCurrentUser() {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
        return JSON.parse(raw);
    } catch (e) {
        return null;
    }
}

function isAuthenticated() {
    return !!getToken() && !!getCurrentUser();
}

function hasRole(roleName) {
    const user = getCurrentUser();
    if (!user || !user.roles) return false;
    return user.roles.includes(roleName);
}

function isAdmin() {
    return hasRole('ROLE_ADMIN');
}

function isManager() {
    return hasRole('ROLE_MANAGER');
}

function isEmployee() {
    return hasRole('ROLE_EMPLOYEE');
}

function isAuditor() {
    return hasRole('ROLE_AUDITOR');
}

function requireAuth(allowedRoles = []) {
    if (!isAuthenticated()) {
        window.location.href = '/login.html';
        return false;
    }
    if (allowedRoles.length > 0) {
        const user = getCurrentUser();
        const hasAnyAllowed = allowedRoles.some(r => user.roles && user.roles.includes(r));
        if (!hasAnyAllowed && !isAdmin()) {
            alert('Access Denied: You do not have permissions to access this page.');
            window.location.href = '/files.html';
            return false;
        }
    }
    return true;
}

function logout() {
    const token = getToken();
    if (token) {
        fetch('/api/auth/logout', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        }).catch(err => console.error(err));
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    window.location.href = '/login.html';
}

async function apiFetch(url, options = {}) {
    const token = getToken();
    const headers = options.headers ? new Headers(options.headers) : new Headers();

    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const config = {
        ...options,
        headers: headers
    };

    try {
        const response = await fetch(url, config);

        if (response.status === 401) {
            logout();
            throw new Error('Session expired or unauthorized. Please log in again.');
        }

        return response;
    } catch (err) {
        throw err;
    }
}

function showAlert(message, type = 'danger', containerId = 'alert-container') {
    const container = document.getElementById(containerId);
    if (!container) return;

    const icon = type === 'success' ? 'check-circle' : (type === 'warning' ? 'exclamation-triangle' : 'exclamation-circle');
    container.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            <i class="fas fa-${icon} me-2"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}
