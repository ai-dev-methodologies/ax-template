import { describe, it, expect, vi } from "vitest";
import { render, screen, within, act, fireEvent } from "@testing-library/react";
import TextField from "@/components/showcase/ax-blocks/form-field";
import { GridStatus } from "@/components/showcase/ax-blocks/data-grid";
import AvatarGroup from "@/components/showcase/ax-blocks/avatar-group";
import CodeSnippet from "@/components/showcase/ax-blocks/code-snippet";

// Behavioral contracts the adversarial audit found and the passing gates (axe/eslint/tsc/build) miss.
// Each test locks one confirmed fix so it cannot silently regress.

describe("form-field: the block's a11y attributes are authoritative", () => {
  it("caller-supplied aria-describedby cannot detach the error association (spread is first)", () => {
    render(<TextField id="f1" label="Key" error="too short" aria-describedby="attacker-node" />);
    const input = screen.getByRole("textbox");
    expect(input.getAttribute("aria-describedby")).toBe("f1-error");
    expect(input.getAttribute("aria-invalid")).toBe("true");
  });

  it("helper and error coexist — instructions survive an error (WCAG 3.3.2) and describedby names both", () => {
    render(<TextField id="f2" label="Key" helper="Must be 32 chars" error="too short" />);
    expect(screen.getByText("Must be 32 chars")).toBeTruthy();
    expect(screen.getByText("too short")).toBeTruthy();
    expect(screen.getByRole("textbox").getAttribute("aria-describedby")).toBe("f2-helper f2-error");
  });

  it("the required asterisk is decorative (aria-hidden) — required state comes from the input attribute", () => {
    const { container } = render(<TextField id="f3" label="Name" required />);
    const star = container.querySelector('span[aria-hidden="true"]');
    expect(star?.textContent).toContain("*");
    expect(screen.getByRole("textbox").hasAttribute("required")).toBe(true);
  });

  it("aria-describedby is absent when neither helper nor error is set", () => {
    render(<TextField id="f4" label="Plain" />);
    expect(screen.getByRole("textbox").hasAttribute("aria-describedby")).toBe(false);
  });
});

describe("data-grid GridStatus: status semantics reach assistive tech", () => {
  it("has role=status and an aria-label equal to the visible text (WCAG 4.1.3 + 2.5.3)", () => {
    render(<GridStatus status="success">Settled</GridStatus>);
    const pill = screen.getByRole("status");
    expect(pill.getAttribute("aria-label")).toBe("Settled");
  });

  it("falls back to the status keyword as the accessible name for non-string children", () => {
    render(
      <GridStatus status="warning">
        <svg aria-hidden="true" />
      </GridStatus>,
    );
    expect(screen.getByRole("status").getAttribute("aria-label")).toBe("warning");
  });
});

describe("avatar-group: the full member name reaches assistive tech in both paths", () => {
  it("a member without a photo exposes the full name (not just initials) via role=img aria-label", () => {
    render(<AvatarGroup members={[{ name: "Linus Torvalds" }]} />);
    const avatar = screen.getByRole("img");
    expect(avatar.getAttribute("aria-label")).toBe("Linus Torvalds");
    // initials are decorative
    expect(within(avatar).getByText("LT").getAttribute("aria-hidden")).toBe("true");
  });

  it("the +N overflow chip is announced as '{n} more', not a bare '+n'", () => {
    render(<AvatarGroup max={1} members={[{ name: "A B" }, { name: "C D" }, { name: "E F" }]} />);
    expect(screen.getByLabelText("2 more")).toBeTruthy();
  });

  it("shows the photo, falls back to initials on load error, and reloads when a new src lands at the same slot", () => {
    const { container, rerender } = render(
      <AvatarGroup members={[{ name: "Ada Lovelace", src: "https://example.test/broken.png" }]} />,
    );
    const img = container.querySelector("img");
    expect(img).not.toBeNull();
    act(() => {
      fireEvent.error(img!);
    });
    expect(container.querySelector("img")).toBeNull(); // fell back to initials
    // a DIFFERENT member with a valid src now occupies the same index — the prior error must not stick
    rerender(<AvatarGroup members={[{ name: "Grace Hopper", src: "https://example.test/valid.png" }]} />);
    expect(container.querySelector("img")).not.toBeNull();
  });
});

describe("code-snippet: a single copy-announcement channel", () => {
  it("has no redundant aria-live region (announces via the focused button's name change)", () => {
    const { container } = render(<CodeSnippet code="const x = 1" language="typescript" />);
    expect(container.querySelector("[aria-live]")).toBeNull();
    expect(container.querySelector('[role="status"]')).toBeNull();
    const btn = screen.getByRole("button");
    expect(btn.getAttribute("aria-label")).toBeNull(); // name derives from the visible text
    expect(btn.textContent).toContain("Copy");
  });

  it("after copy, shows 'Copied' with the check glyph aria-hidden so the announced name stays clean", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });
    const { container } = render(<CodeSnippet code="const x = 1" />);
    const btn = screen.getByRole("button");
    await act(async () => {
      fireEvent.click(btn);
    });
    expect(writeText).toHaveBeenCalledWith("const x = 1");
    expect(btn.textContent).toContain("Copied");
    // the check glyph must stay out of the accessible name (regression path: dropping aria-hidden)
    const glyph = btn.querySelector('span[aria-hidden="true"]');
    expect(glyph?.textContent).toContain("✓");
    // still a single channel — copying did not introduce a live region
    expect(container.querySelector("[aria-live]")).toBeNull();
    expect(container.querySelector('[role="status"]')).toBeNull();
  });
});
