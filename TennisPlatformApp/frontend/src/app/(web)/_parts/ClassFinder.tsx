"use client";

import { useState } from "react";
import { Copy } from "./Copy";
import { type Format, type Learner, recommendClass, trialHref } from "./school";

const LEARNERS: { value: Learner; label: string }[] = [
  { value: "me", label: "Para mí" },
  { value: "child", label: "Para mi hijo o hija" },
];

const FORMATS: { value: Format; label: string }[] = [
  { value: "group", label: "En grupo" },
  { value: "private", label: "Clase particular" },
];

/**
 * "¿Qué clase es para mí?" (31-fase31, decision 21). Two questions suggest one of the classes,
 * and the trial button asks for it by name. Nothing is kept or sent anywhere.
 */
export function ClassFinder() {
  const [learner, setLearner] = useState<Learner>();
  const [format, setFormat] = useState<Format>();
  const suggestion = learner && format ? recommendClass(learner, format) : undefined;

  return (
    <div className="mt-16 grid gap-10 rounded-lg bg-(--site-surface) p-6 sm:p-10 lg:grid-cols-[1fr_1.1fr] lg:gap-16">
      <div className="flex flex-col gap-7">
        <h3 className="font-display text-4xl font-bold">¿Qué clase es para mí?</h3>
        <Choice name="learner" legend="¿Para quién es?" options={LEARNERS} value={learner} onChange={setLearner} />
        <Choice name="format" legend="¿Cómo la prefieres?" options={FORMATS} value={format} onChange={setFormat} />
      </div>
      <div aria-live="polite" className="flex">
        {suggestion ? (
          <div key={suggestion.lesson.name} className="site-appear flex w-full flex-col gap-4 border-t-[3px] border-(--site-text) pt-6">
            <p className="text-(--site-muted)">La clase para ti</p>
            <p className="font-display text-6xl font-bold leading-none">{suggestion.lesson.name}</p>
            <p className="text-[17px] leading-relaxed"><Copy text={suggestion.lesson.summary} /></p>
            <p className="text-[17px]">
              <Copy text={suggestion.lesson.when} />. <span className="font-semibold"><Copy text={suggestion.lesson.price} /></span>
            </p>
            <a
              href={trialHref(undefined, suggestion.message)}
              className="mt-2 inline-flex min-h-13 items-center justify-center gap-2.5 self-start rounded-lg bg-ball px-6 text-[17px] font-semibold text-navy hover:bg-(--site-text) hover:text-(--site-bg)"
            >
              Pedir la prueba de {suggestion.lesson.name.toLowerCase()}
            </a>
          </div>
        ) : (
          <p className="self-center text-lg leading-relaxed text-(--site-muted)">
            Responde a las dos preguntas y te decimos qué clase te va, con su horario y su precio.
          </p>
        )}
      </div>
    </div>
  );
}

type ChoiceProps<T extends string> = {
  name: string;
  legend: string;
  options: { value: T; label: string }[];
  value: T | undefined;
  onChange: (value: T) => void;
};

/** Real radio buttons, so the keyboard and screen readers work as they expect. */
function Choice<T extends string>({ name, legend, options, value, onChange }: ChoiceProps<T>) {
  return (
    <fieldset className="flex flex-col gap-3">
      <legend className="mb-3 text-lg font-semibold">{legend}</legend>
      <div className="flex flex-wrap gap-3">
        {options.map((option) => (
          <label
            key={option.value}
            className="flex min-h-12 cursor-pointer items-center rounded-full border-2 border-(--site-line) px-5 text-[17px] font-semibold transition-colors hover:border-(--site-text) has-checked:border-(--site-text) has-checked:bg-(--site-text) has-checked:text-(--site-bg) has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-(--site-link)"
          >
            <input
              type="radio"
              name={name}
              value={option.value}
              checked={value === option.value}
              onChange={() => onChange(option.value)}
              className="sr-only"
            />
            {option.label}
          </label>
        ))}
      </div>
    </fieldset>
  );
}
