import { CORRELATION_HEADER } from "@/shared/api/client";
import type { components } from "@/shared/api/schema";

type ProblemDetails = components["schemas"]["ProblemDetails"];

export interface FieldError {
  field: string;
  message: string;
}

/** Every failed call ends up as one of these, whatever the endpoint. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string | undefined;
  readonly fieldErrors: FieldError[];
  /** What the backend's log knows this request by. */
  readonly correlationId: string | undefined;

  constructor(
    status: number, code: string | undefined, detail: string | undefined, fieldErrors: FieldError[] = [],
    correlationId?: string,
  ) {
    super(detail ?? `HTTP ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.correlationId = correlationId;
  }

  /** A 409 means what is on screen is out of date: re-read before trying again. */
  get isStale(): boolean {
    return this.status === 409;
  }

  static fromProblem(status: number, body: unknown, correlationId?: string): ApiError {
    const problem = isProblem(body) ? body : undefined;
    return new ApiError(status, problem?.code, problem?.detail ?? problem?.title, problem?.errors ?? [], correlationId);
  }
}

function isProblem(body: unknown): body is ProblemDetails {
  return typeof body === "object" && body !== null && ("code" in body || "title" in body);
}

/** The backend's message for one field of a rejected body, to show under that field. */
export function fieldErrorOf(error: unknown, field: string): string | undefined {
  return error instanceof ApiError ? error.fieldErrors.find((e) => e.field === field)?.message : undefined;
}

/**
 * Turns an openapi-fetch result into its data, or throws. Components never see `error`
 * objects: TanStack Query hands them the ApiError.
 */
export function unwrap<T>(result: { data?: T; error?: unknown; response: Response }): T {
  if (result.error !== undefined || !result.response.ok) {
    throw ApiError.fromProblem(result.response.status, result.error,
      result.response.headers.get(CORRELATION_HEADER) ?? undefined);
  }
  return result.data as T;
}
