"use client";
/*
---
template_id: L2/blocks/rich-text-editor
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Tiptap Editor — official React integration. `useEditor` + `EditorContent` from @tiptap/react, StarterKit from @tiptap/starter-kit; `immediatelyRender: false` is the documented Next.js App Router / React 19 SSR pattern that prevents hydration mismatch."
    url: "https://tiptap.dev/docs/editor/getting-started/install/nextjs"
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — Toolbar pattern. role=toolbar with an accessible name, toggle controls expose state via aria-pressed; here the toolbar is a labelled group of @ax/ui Buttons each carrying aria-pressed for its active mark/node."
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/toolbar/"
rationale: "ax-native L2 rich-text editor codified to fill the long-form authoring gap (the editorial/publishing persona had no body-content primitive). Composes @ax/ui Button for the toolbar (NOT a bespoke button), so it re-skins with the host theme; the editor surface, toolbar chrome, and focus rings bind to the shared design tokens (--radius / --ring / border / muted / foreground), never raw hex. WCAG: the toolbar is role=toolbar with an aria-label, each control is a real <button> with an aria-label + aria-pressed reflecting editor.isActive(...), and the contenteditable area is labelled via aria-label/aria-labelledby and is fully keyboard-operable (Tiptap/ProseMirror native). Motion is limited to token transitions on the toolbar buttons; respects prefers-reduced-motion through the @ax/ui Button base. Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: [@ax/ui]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react';
import { useEditor, EditorContent, useEditorState, type Editor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import {
  Bold,
  Italic,
  Strikethrough,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Quote,
  Undo2,
  Redo2,
} from 'lucide-react';
import { Button } from '@ax/ui';

export interface RichTextEditorProps {
  /** Initial HTML content. Controlled-on-mount only; later prop changes do not reset the doc. */
  value?: string;
  /** Fired with the serialized HTML on every document change. */
  onChange?: (html: string) => void;
  /** Placeholder-style empty hint rendered when the doc is empty (visual only). */
  placeholder?: string;
  /** Accessible name for the editable region. Required for WCAG 4.1.2. */
  ariaLabel: string;
  /** Disable all editing + toolbar controls. */
  disabled?: boolean;
  className?: string;
}

interface ToolbarControl {
  key: string;
  label: string;
  Icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean }>;
  /** is this control's mark/node currently active? */
  isActive: (e: Editor) => boolean;
  /** run the command */
  run: (e: Editor) => void;
  /** is the command currently runnable? (e.g. undo with empty history) */
  canRun: (e: Editor) => boolean;
}

// The toolbar control set. Each entry maps a StarterKit command to a labelled,
// token-driven @ax/ui Button. Labels are Korean (editorial persona copy).
const CONTROLS: ToolbarControl[] = [
  {
    key: 'bold',
    label: '굵게',
    Icon: Bold,
    isActive: (e) => e.isActive('bold'),
    run: (e) => e.chain().focus().toggleBold().run(),
    canRun: (e) => e.can().chain().focus().toggleBold().run(),
  },
  {
    key: 'italic',
    label: '기울임',
    Icon: Italic,
    isActive: (e) => e.isActive('italic'),
    run: (e) => e.chain().focus().toggleItalic().run(),
    canRun: (e) => e.can().chain().focus().toggleItalic().run(),
  },
  {
    key: 'strike',
    label: '취소선',
    Icon: Strikethrough,
    isActive: (e) => e.isActive('strike'),
    run: (e) => e.chain().focus().toggleStrike().run(),
    canRun: (e) => e.can().chain().focus().toggleStrike().run(),
  },
  {
    key: 'h2',
    label: '제목 2',
    Icon: Heading2,
    isActive: (e) => e.isActive('heading', { level: 2 }),
    run: (e) => e.chain().focus().toggleHeading({ level: 2 }).run(),
    canRun: (e) => e.can().chain().focus().toggleHeading({ level: 2 }).run(),
  },
  {
    key: 'h3',
    label: '제목 3',
    Icon: Heading3,
    isActive: (e) => e.isActive('heading', { level: 3 }),
    run: (e) => e.chain().focus().toggleHeading({ level: 3 }).run(),
    canRun: (e) => e.can().chain().focus().toggleHeading({ level: 3 }).run(),
  },
  {
    key: 'bulletList',
    label: '글머리 목록',
    Icon: List,
    isActive: (e) => e.isActive('bulletList'),
    run: (e) => e.chain().focus().toggleBulletList().run(),
    canRun: (e) => e.can().chain().focus().toggleBulletList().run(),
  },
  {
    key: 'orderedList',
    label: '번호 목록',
    Icon: ListOrdered,
    isActive: (e) => e.isActive('orderedList'),
    run: (e) => e.chain().focus().toggleOrderedList().run(),
    canRun: (e) => e.can().chain().focus().toggleOrderedList().run(),
  },
  {
    key: 'blockquote',
    label: '인용구',
    Icon: Quote,
    isActive: (e) => e.isActive('blockquote'),
    run: (e) => e.chain().focus().toggleBlockquote().run(),
    canRun: (e) => e.can().chain().focus().toggleBlockquote().run(),
  },
  {
    key: 'undo',
    label: '실행 취소',
    Icon: Undo2,
    isActive: () => false,
    run: (e) => e.chain().focus().undo().run(),
    canRun: (e) => e.can().chain().focus().undo().run(),
  },
  {
    key: 'redo',
    label: '다시 실행',
    Icon: Redo2,
    isActive: () => false,
    run: (e) => e.chain().focus().redo().run(),
    canRun: (e) => e.can().chain().focus().redo().run(),
  },
];

/** A single toolbar button — re-renders only on its own active/can change. */
function ToolbarButton({ editor, control }: { editor: Editor; control: ToolbarControl }) {
  const { active, enabled } = useEditorState({
    editor,
    selector: ({ editor: e }) => ({
      active: control.isActive(e),
      enabled: control.canRun(e),
    }),
  });
  const { Icon, label } = control;
  return (
    <Button
      type="button"
      variant={active ? 'secondary' : 'ghost'}
      size="icon"
      aria-label={label}
      aria-pressed={active}
      disabled={!enabled}
      onClick={() => control.run(editor)}
      className="h-9 w-9 data-[active=true]:bg-secondary"
      data-active={active}
    >
      <Icon aria-hidden className="h-4 w-4" />
    </Button>
  );
}

/**
 * Token-driven rich-text editor. Tiptap StarterKit content, a labelled
 * role=toolbar of @ax/ui Buttons, and an accessible contenteditable surface.
 * SSR-safe via `immediatelyRender: false` (Next.js App Router / React 19).
 */
export function RichTextEditor({
  value = '',
  onChange,
  placeholder,
  ariaLabel,
  disabled = false,
  className,
}: RichTextEditorProps) {
  const editor = useEditor({
    extensions: [StarterKit],
    content: value,
    editable: !disabled,
    immediatelyRender: false,
    editorProps: {
      attributes: {
        'aria-label': ariaLabel,
        role: 'textbox',
        'aria-multiline': 'true',
        class:
          'ax-rte-content min-h-[16rem] w-full px-4 py-3 text-base leading-relaxed text-foreground focus:outline-none',
      },
    },
    onUpdate: ({ editor: e }) => onChange?.(e.getHTML()),
  });

  // Keep `editable` in sync if the disabled prop changes after mount.
  React.useEffect(() => {
    editor?.setEditable(!disabled);
  }, [editor, disabled]);

  if (!editor) {
    // Pre-hydration / not-yet-initialized placeholder keeps layout stable.
    return (
      <div
        className={[
          'overflow-hidden rounded-[var(--radius)] border border-input bg-background',
          className,
        ]
          .filter(Boolean)
          .join(' ')}
      >
        <div className="min-h-[16rem] px-4 py-3 text-base text-muted-foreground">
          {placeholder ?? '본문을 입력하세요…'}
        </div>
      </div>
    );
  }

  const isEmpty = editor.isEmpty;

  return (
    <div
      className={[
        'overflow-hidden rounded-[var(--radius)] border border-input bg-background',
        'focus-within:border-ring focus-within:ring-2 focus-within:ring-ring/40',
        disabled ? 'opacity-60' : '',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      <div
        role="toolbar"
        aria-label="본문 서식 도구 모음"
        className="flex flex-wrap items-center gap-0.5 border-b border-border bg-muted/40 px-2 py-1.5"
      >
        {CONTROLS.map((control) => (
          <ToolbarButton key={control.key} editor={editor} control={control} />
        ))}
      </div>

      <div className="relative">
        {isEmpty && placeholder ? (
          <p
            aria-hidden="true"
            className="pointer-events-none absolute left-4 top-3 select-none text-base text-muted-foreground/70"
          >
            {placeholder}
          </p>
        ) : null}
        <EditorContent editor={editor} />
      </div>
    </div>
  );
}

export default RichTextEditor;
