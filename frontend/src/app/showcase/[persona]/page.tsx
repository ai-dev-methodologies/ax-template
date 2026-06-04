import { notFound } from "next/navigation";
import personas from "@/components/showcase/personas-showcase.json";
import { PersonaShowcase, type PersonaData } from "@/components/showcase/persona-showcase";

const DATA = personas as Record<string, PersonaData>;

export function generateStaticParams() {
  return Object.keys(DATA).map((persona) => ({ persona }));
}

export async function generateMetadata({ params }: { params: Promise<{ persona: string }> }) {
  const { persona } = await params;
  const data = DATA[persona];
  return { title: data ? `${data.name} — ax-template showcase` : "ax-template showcase" };
}

export default async function PersonaPage({ params }: { params: Promise<{ persona: string }> }) {
  const { persona } = await params;
  const data = DATA[persona];
  if (!data) notFound();
  return <PersonaShowcase slug={persona} data={data} />;
}
