/**
 * FIXTURE: combobox-respects-hangul-ime-composition/pass
 * Demonstrates CORRECT pattern: filtering is suppressed during IME composition.
 * Uses nativeEvent.isComposing + onCompositionEnd to fire the filter only after
 * the Korean syllable block is fully composed.
 */
"use client";

import { useState, useRef } from "react";

interface ComboboxProps {
  options: string[];
  onSelect: (value: string) => void;
}

export default function Combobox({ options, onSelect }: ComboboxProps) {
  const [query, setQuery] = useState("");
  const [filtered, setFiltered] = useState<string[]>([]);
  const composingRef = useRef(false);

  // CORRECT: track IME composition state
  function handleCompositionStart() {
    composingRef.current = true;
  }

  function handleCompositionEnd(e: React.CompositionEvent<HTMLInputElement>) {
    composingRef.current = false;
    // Fire the filter once the syllable is fully committed
    const val = (e.target as HTMLInputElement).value;
    setFiltered(options.filter(o => o.includes(val)));
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    setQuery(val);
    // CORRECT: skip filtering while the IME is still composing
    if (composingRef.current) return;
    setFiltered(options.filter(o => o.includes(val)));
  }

  return (
    <div>
      <input
        type="text"
        value={query}
        onChange={handleChange}
        onCompositionStart={handleCompositionStart}
        onCompositionEnd={handleCompositionEnd}
        placeholder="Search..."
      />
      <ul>
        {filtered.map(opt => (
          <li key={opt} onClick={() => onSelect(opt)}>{opt}</li>
        ))}
      </ul>
    </div>
  );
}
