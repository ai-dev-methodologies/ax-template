'use client';

import React from 'react';
import { CodeSnippet, GridStatus, type DataGridStatus } from '@ax/blocks';
import { formatRequest, formatResponse } from '@/lib/http-format';
import type { HttpExchange } from '@/lib/api/rawFetch';

/**
 * The console's signature surface: render a captured {@link HttpExchange} as the
 * ACTUAL HTTP request + the ACTUAL HTTP response, each in a catalog @ax/blocks
 * CodeSnippet (copy-able, semantic <pre><code>, a11y). A status pill (the catalog
 * GridStatus, token-driven) flags 2xx/4xx/5xx. App-local composition around the
 * catalog block — it adds zero new primitives.
 */

function statusKind(status: number): DataGridStatus {
  if (status >= 200 && status < 300) return 'success';
  if (status >= 400 && status < 500) return 'warning';
  if (status >= 500) return 'danger';
  return 'info';
}

interface ExchangeViewProps {
  exchange: HttpExchange<unknown>;
  /** Optional override for the request snippet filename label. */
  requestLabel?: string;
  /** Optional override for the response snippet filename label. */
  responseLabel?: string;
}

export function ExchangeView({
  exchange,
  requestLabel = 'request.http',
  responseLabel = 'response.http',
}: ExchangeViewProps) {
  const { response } = exchange;
  return (
    <div className="ax-code-pane space-y-3">
      <div className="flex items-center gap-2">
        <GridStatus status={statusKind(response.status)}>
          {`${response.status} ${response.statusText}`.trim()}
        </GridStatus>
        <span className="font-mono text-[0.7rem] text-muted-foreground">
          {exchange.request.method} {exchange.request.path}
        </span>
      </div>
      <CodeSnippet code={formatRequest(exchange.request)} language="http" filename={requestLabel} />
      <CodeSnippet code={formatResponse(response)} language="http" filename={responseLabel} />
    </div>
  );
}
