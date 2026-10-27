"use client";

import { useRef } from "react";
import { useMyPhoto, useRemovePhoto, useUploadPhoto } from "@/modules/student/api";
import { Avatar } from "@/shared/ui/Avatar";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/Notice";

/** The student's own photo (39-fase25.5): only they and their teacher see it. */
export function PhotoPicker({ name, hasPhoto }: { name: string | null; hasPhoto: boolean }) {
  const input = useRef<HTMLInputElement>(null);
  const photo = useMyPhoto(hasPhoto);
  const upload = useUploadPhoto();
  const remove = useRemovePhoto();

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-4">
        <Avatar name={name} src={hasPhoto ? photo.data : null} size="lg" />
        <div className="flex flex-col gap-2">
          <p className="text-sm text-muted">Tu foto sólo la ve tu profesor.</p>
          <div className="flex flex-wrap gap-2">
            <Button variant="quiet" pending={upload.isPending} onClick={() => input.current?.click()}>
              {hasPhoto ? "Cambiar foto" : "Subir foto"}
            </Button>
            {hasPhoto ? (
              <Button variant="quiet" pending={remove.isPending} onClick={() => remove.mutate()}>Quitar</Button>
            ) : null}
          </div>
        </div>
        <input ref={input} type="file" accept="image/*" className="sr-only" aria-label="Elegir una foto"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) upload.mutate(file);
            event.target.value = "";
          }} />
      </div>
      <ErrorNotice error={upload.error ?? remove.error} />
    </div>
  );
}
