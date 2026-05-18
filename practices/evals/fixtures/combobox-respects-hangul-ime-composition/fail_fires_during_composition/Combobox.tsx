/**
 * FIXTURE: combobox-respects-hangul-ime-composition/fail_fires_during_composition
 * Demonstrates WRONG pattern: onChange fires during IME composition (compositionupdate).
 * Korean (한글) input via IME fires multiple onChange events while the user is still
 * composing a syllable block. Filtering on these partial values produces wrong results.
 * Guard must catch: onChange without isComposing guard.
 */
"use client";

import { useState } from "react";

interface ComboboxProps {
  options: string[];
  onSelect: (value: string) => void;
}

export default function Combobox({ options, onSelect }: ComboboxProps) {
  const [query, setQuery] = useState("");
  const [filtered, setFiltered] = useState<string[]>([]);

  // VIOLATION: onChange fires on every compositionupdate event during Korean IME input.
  // User types '한' (ㅎ→하→한) — three onChange events fire before the syllable is done.
  // Each partial keystroke triggers a filter pass with incomplete input ("ㅎ", "하", "한").
  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    setQuery(val);
    // BUG: no e.nativeEvent.isComposing check — filters fire during IME composition
    setFiltered(options.filter(o => o.includes(val)));
  }

  return (
    <div>
      <input
        type="text"
        value={query}
        onChange={handleChange}
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
