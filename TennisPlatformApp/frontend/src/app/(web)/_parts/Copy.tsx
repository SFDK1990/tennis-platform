import { placeholderParts } from "./school";

/** A text whose [placeholders] look unfinished on purpose, so none is published by mistake. */
export function Copy({ text }: { text: string }) {
  return placeholderParts(text).map((part, index) =>
    part.pending ? (
      <mark key={index} className="bg-transparent text-inherit underline decoration-dashed decoration-2 underline-offset-4">
        {part.text}
      </mark>
    ) : (
      part.text
    ),
  );
}
