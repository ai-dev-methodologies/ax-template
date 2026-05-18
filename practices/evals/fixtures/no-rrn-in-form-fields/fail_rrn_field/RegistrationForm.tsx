/**
 * FIXTURE: no-rrn-in-form-fields/fail_rrn_field
 * Demonstrates WRONG pattern: a form with an RRN (주민등록번호) input field.
 * RRN is Sensitive Personal Information under 개인정보보호법. Collecting it
 * via a form without explicit legal basis and consent gate is a compliance violation.
 * Guard must catch: input element with name="rrn" or name includes "주민등록번호".
 */
"use client";

import { useState } from "react";

interface RegistrationData {
  name: string;
  email: string;
  rrn: string; // VIOLATION: RRN should never appear as a default form field
}

export default function RegistrationForm() {
  const [form, setForm] = useState<RegistrationData>({
    name: "",
    email: "",
    rrn: "",
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
      {/* VIOLATION: RRN input field — 개인정보보호법 prohibits default collection */}
      <div>
        <label htmlFor="rrn">주민등록번호</label>
        <input
          id="rrn"
          name="rrn"
          type="text"
          placeholder="000000-0000000"
          value={form.rrn}
          onChange={(e) => setForm(f => ({ ...f, rrn: e.target.value }))}
        />
      </div>
      <button type="submit">Register</button>
    </form>
  );
}
