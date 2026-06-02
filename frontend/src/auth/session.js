const STORAGE_KEY = "uconkma.session";

export function getSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setSession(session) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  window.dispatchEvent(new Event("ucon-session-changed"));
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY);
  window.dispatchEvent(new Event("ucon-session-changed"));
}

export function isAdmin(session = getSession()) {
  return session?.role === "ADMIN";
}

export function isStudent(session = getSession()) {
  return session?.role === "STUDENT";
}

