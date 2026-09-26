import { MutationCache, QueryClient } from "@tanstack/react-query";
import { ApiError } from "@/shared/api/errors";

/**
 * After any write, successful or not, whatever is on screen is re-read. That is the whole
 * cache policy: after a success the data changed, and after a 409 it was already stale. With a
 * single teacher and a handful of screens, being precise about which queries each write
 * touches would buy nothing but places to forget one.
 */
export function createQueryClient(): QueryClient {
  const client: QueryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        // An answer from the API will not change by asking again; a network blip might.
        retry: (failures, error) => !(error instanceof ApiError) && failures < 2,
      },
    },
    mutationCache: new MutationCache({
      onSettled: () => client.invalidateQueries(),
    }),
  });
  return client;
}
