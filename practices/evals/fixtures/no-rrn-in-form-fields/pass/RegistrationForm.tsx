/**
 * FIXTURE: no-rrn-in-form-fields/pass
 * Demonstrates CORRECT pattern: registration form collects only minimum required
 * personal information. No RRN field. If identity verification is needed, it is
 * delegated to a dedicated KYC flow (not a default form field).
 */
"use client";

import { useState } from "react";

interface RegistrationData {
  name: string;
  email: string;
  // CORRECT: no rrn field — identity verification (if needed) goes through
  // a dedicated, legally-reviewed KYC flow with explicit consent gates
}

export default function RegistrationForm() {
  const [form, setForm] = useState<RegistrationData>({
    name: "",
    email: "",
  });

  return (
    <form>
      <div>
        <label htmlFor="name">Name</label>
        <input
          id="name"
          name="name"
          value={form.name}
          onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
        />
      </div>
      <div>
        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          value={form.email}
          onChange={(e) => setForm(f => ({ ...f, email: e.target.value }))}
        />
      </div>
      {/* CORRECT: no RRN field. If identity verification is required, it is
          rendered via a separate KYC component with explicit legal basis display. */}
      <button type="submit">Register</button>
    </form>
  );
}
