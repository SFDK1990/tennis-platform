import { PassTypesManager } from "@/modules/passes/components/PassTypesManager";

/** The teacher's passes (Fase 27): the types they sell and whether booking needs one. */
export default function PassesPage() {
  return (
    <>
      <h1 className="font-display text-4xl font-semibold leading-none">Bonos</h1>
      <PassTypesManager />
    </>
  );
}
