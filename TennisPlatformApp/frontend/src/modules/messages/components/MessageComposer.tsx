"use client";

import { useId, useState, type FormEvent } from "react";
import { MAX_BODY, MAX_SUBJECT, useSendMessage, type MessageDraft } from "@/modules/messages/api";
import { useLevels } from "@/shared/levels";
import { Button } from "@/shared/ui/Button";
import { CONTROL_CLASS, Field } from "@/shared/ui/Field";
import { ErrorNotice, Notice } from "@/shared/ui/Notice";

/** Who the message goes to, as the form offers it. */
export interface AudienceOption {
  key: string;
  label: string;
  audience: Pick<MessageDraft, "audience" | "levelId" | "lessonId">;
}

/** Everybody and each level: what the teacher's own Mensajes screen offers. */
export function useGeneralAudiences(): AudienceOption[] {
  const levels = useLevels();
  return [
    { key: "ALL", label: "Todos los alumnos", audience: { audience: "ALL" } },
    ...(levels.data ?? []).map((level) => ({
      key: level.id,
      label: `Nivel ${level.name}`,
      audience: { audience: "LEVEL" as const, levelId: level.id },
    })),
  ];
}

/**
 * Writing to students (37-fase24, decisions 1 to 4). The recipients are fixed when it is sent and
 * the email cannot be taken back, which the form says next to the button.
 */
export function MessageComposer({ options, title = "Escribir un mensaje" }: { options: AudienceOption[]; title?: string }) {
  const send = useSendMessage();
  const headingId = useId();
  const [chosen, setChosen] = useState<string | null>(null);
  const [body, setBody] = useState("");
  const selected = options.find((option) => option.key === chosen) ?? options[0];

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const subject = String(new FormData(form).get("subject")).trim();
    if (!selected || !subject || !body.trim()) return;
    send.mutate({ ...selected.audience, subject, body: body.trim() }, {
      onSuccess: () => {
        form.reset();
        setBody("");
      },
    });
  }

  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-4 rounded-xl border border-line bg-paper p-5">
      <h2 id={headingId} className="font-display text-2xl font-semibold leading-none">{title}</h2>
      <form onSubmit={submit} className="flex flex-col gap-4">
        {options.length > 1 ? (
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-1 text-sm font-semibold">Para</legend>
            <div className="flex flex-wrap gap-2">
              {options.map((option) => (
                <label key={option.key}
                  className="flex min-h-11 cursor-pointer items-center gap-2 rounded-lg border-[1.5px] border-line px-3.5 has-checked:border-court has-checked:bg-court-tint has-checked:font-semibold has-checked:text-court">
                  <input type="radio" name="audience" value={option.key} checked={option.key === selected?.key}
                    onChange={() => setChosen(option.key)} className="accent-court" />
                  {option.label}
                </label>
              ))}
            </div>
          </fieldset>
        ) : selected ? (
          <p className="text-sm"><span className="font-semibold">Para:</span> {selected.label}</p>
        ) : null}
        <Field label="Asunto" name="subject" required maxLength={MAX_SUBJECT} />
        <BodyArea value={body} onChange={setBody} />
        <ErrorNotice error={send.error} />
        {send.isSuccess ? (
          <Notice tone="success">
            Enviado a {send.data.recipientCount} {send.data.recipientCount === 1 ? "alumno" : "alumnos"}.
          </Notice>
        ) : null}
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <Button type="submit" pending={send.isPending}>Enviar</Button>
          <p className="text-sm text-muted">Les llega por correo y a sus avisos. El correo no se puede retirar.</p>
        </div>
      </form>
    </section>
  );
}

function BodyArea({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  const id = useId();
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold">Mensaje</label>
      <textarea id={id} name="body" required rows={6} maxLength={MAX_BODY} value={value}
        onChange={(event) => onChange(event.target.value)} aria-describedby={`${id}-count`}
        className={`${CONTROL_CLASS} min-h-36 py-2`} />
      <span id={`${id}-count`} className="self-end text-xs text-muted">{value.length} de {MAX_BODY}</span>
    </div>
  );
}
