import React from 'react';
import {
  InterfacesCard,
  InterfacesCardContent,
  InterfacesCardHeader,
  InterfacesCardTitle,
} from '@ax/blocks';

/**
 * A tabular-figure metric tile. Composes the catalog InterfacesCard family — no
 * app-local card is defined. The big value renders in an `.ax-money` run so the
 * persona's tabular-nums applies (digit columns align across tiles); the trend /
 * sublabel sits beneath in muted text. The defining fintech-trust surface:
 * precise money, no jitter.
 */
export function MetricTile({
  label,
  value,
  sublabel,
  icon,
}: {
  label: string;
  value: string;
  sublabel?: string;
  icon?: React.ReactNode;
}) {
  return (
    <InterfacesCard className="shadow-sm">
      <InterfacesCardHeader className="!border-b-0 pb-0">
        <div className="flex items-center justify-between gap-2">
          <InterfacesCardTitle className="!text-sm !font-medium text-muted-foreground">
            {label}
          </InterfacesCardTitle>
          {icon ? <span aria-hidden className="text-muted-foreground/70">{icon}</span> : null}
        </div>
      </InterfacesCardHeader>
      <InterfacesCardContent className="pt-2">
        <p className="ax-money text-2xl font-semibold leading-tight tracking-tight text-foreground">
          {value}
        </p>
        {sublabel ? (
          <p className="ax-money mt-1 text-xs text-muted-foreground">{sublabel}</p>
        ) : null}
      </InterfacesCardContent>
    </InterfacesCard>
  );
}
