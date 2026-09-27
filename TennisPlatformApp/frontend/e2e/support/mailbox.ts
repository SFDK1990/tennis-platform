import { expect } from "@playwright/test";
import { MAILPIT } from "./stack";

interface Summary {
  ID: string;
}

/**
 * The link that actually arrived by email, read through Mailpit's API: the raw token exists
 * nowhere else, since the database stores only its hash. Returned as path and query, so it is
 * opened on the frontend under test whatever base URL the backend put in the message.
 */
export async function linkSentTo(email: string, path: "/verify-email" | "/reset-password"): Promise<string> {
  let link: string | undefined;
  await expect
    .poll(async () => {
      const search = await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${email}"`)}`);
      const { messages } = (await search.json()) as { messages: Summary[] };
      for (const { ID } of messages) {
        const message = (await (await fetch(`${MAILPIT}/api/v1/message/${ID}`)).json()) as { Text: string };
        const found = message.Text.match(new RegExp(`https?://\\S*(${path}\\?token=[\\w-]+)`));
        if (found) {
          link = found[1];
          return true;
        }
      }
      return false;
    }, { message: `no email with a ${path} link reached ${email}`, timeout: 15_000 })
    .toBe(true);
  return link!;
}
