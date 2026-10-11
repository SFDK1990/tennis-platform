import type { ApiError } from "@/shared/api/errors";

/**
 * What each business code means to the person in front of the screen. The codes are the
 * contract's (11-contrato-api.md); an unknown one falls back to the backend's own detail.
 */
const MESSAGES: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: "El email o la contraseña no son correctos.",
  AUTH_ACCOUNT_NOT_ACTIVE: "Esta cuenta está desactivada.",
  AUTH_RATE_LIMITED: "Demasiados intentos. Espera un minuto y vuelve a probar.",
  AUTH_WEAK_PASSWORD: "La contraseña es demasiado larga. Elige una más corta.",
  AUTH_INVALID_TOKEN: "El enlace no es válido o ya se ha usado. Pide uno nuevo.",
  AUTH_SESSION_EXPIRED: "Tu sesión ha caducado. Inicia sesión otra vez.",
  AUTH_FORBIDDEN: "Tu cuenta no puede hacer esto.",
  TEACHER_FORBIDDEN: "Sólo el profesor puede hacer esto.",
  VALIDATION_ERROR: "Revisa los datos marcados.",
  DATE_RANGE_INVALID: "El rango de fechas no es válido.",
  FIELD_NOT_APPLICABLE_TO_ROLE: "Ese dato no corresponde a tu tipo de cuenta.",
  CURRENT_PASSWORD_INCORRECT: "La contraseña actual no es correcta.",
  EMAIL_NOT_VERIFIED: "Verifica tu email antes de reservar. Busca el correo que te enviamos.",

  STUDENT_NOT_FOUND: "No hay ningún alumno con ese email.",
  STUDENT_NOT_MANAGED: "Este alumno no está en tu lista, o el profesor aún no te ha añadido.",
  STUDENT_ALREADY_MANAGED: "Este alumno ya está en tu lista.",
  STUDENT_ALREADY_INACTIVE: "Este alumno ya estaba dado de baja.",
  STUDENT_PROFILE_INCOMPLETE: "El alumno todavía no ha rellenado sus datos. Pídele que complete su perfil.",
  STUDENT_LIMIT_REACHED: "Has llegado al límite de alumnos.",
  STUDENT_PROFILE_INVALID: "Revisa tus datos.",
  TEACHER_PROFILE_INVALID: "Revisa tus datos.",

  AVAILABILITY_INVALID: "Ese horario no es válido: revisa que el fin sea posterior al inicio.",
  AVAILABILITY_RULES_OVERLAP: "Dos franjas del mismo día se pisan.",
  AVAILABILITY_EXCEPTION_NOT_FOUND: "Esa excepción ya no existe.",

  LESSON_INVALID: "La clase no es válida: dura múltiplos de 30 minutos y no cruza la medianoche.",
  LESSON_OVERLAP: "Ya tienes otra clase a esa hora.",
  LESSON_OUTSIDE_AVAILABILITY: "Esa hora está fuera de tu disponibilidad. Marca «Crear fuera de horario» si es a propósito.",
  LESSON_IN_THE_PAST: "Esa hora ya ha pasado.",
  LESSON_NOT_FOUND: "Esa clase ya no existe.",
  LESSON_ALREADY_CANCELLED: "La clase ya estaba cancelada.",
  LESSON_ALREADY_FINISHED: "La clase ya ha terminado.",
  LESSON_ALREADY_STARTED: "La clase ya ha empezado.",
  LESSON_CAPACITY_BELOW_BOOKINGS: "Ya hay más alumnos apuntados que esas plazas.",
  LESSON_FULL: "Alguien ha cogido la última plaza. La lista está actualizada.",
  LESSON_NOT_BOOKABLE: "La clase se ha cancelado. La lista está actualizada.",

  BOOKING_ALREADY_EXISTS: "Ya tienes plaza en esta clase.",
  STUDENT_SCHEDULE_OVERLAP: "Ya tienes otra clase a esa hora.",
  BOOKING_NOT_FOUND: "Esa reserva ya no existe.",
  BOOKING_ALREADY_CANCELLED: "La reserva ya estaba cancelada.",
  CANCELLATION_WINDOW_EXPIRED: "Faltan menos de 24 horas: ya no se puede cancelar. Habla con tu profesor.",
  ATTENDANCE_NOT_YET_OPEN: "La asistencia se marca cuando la clase ha empezado.",

  ADMIN_TARGET_NOT_ALLOWED: "Sólo se pueden activar o desactivar cuentas de alumnos.",
  USER_NOT_FOUND: "Esa cuenta ya no existe.",
  ACCOUNT_DELETED: "El alumno borró su cuenta: ya no se puede activar ni desactivar.",
};

/**
 * A failure of the server says nothing useful to the person, so it gives them what to pass on:
 * the start of the id the backend logged the request under.
 */
export function messageFor(error: ApiError): string {
  if (error.status >= 500) {
    const reference = error.correlationId ? ` Código de referencia: ${error.correlationId.slice(0, 8)}.` : "";
    return `Ha fallado algo. Vuelve a probar en un momento.${reference}`;
  }
  return (error.code && MESSAGES[error.code]) || error.message;
}
