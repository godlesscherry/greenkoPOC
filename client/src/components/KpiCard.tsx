interface KpiCardProps {
  title: string;
  value: string;
  subtitle?: string;
}

export function KpiCard({ title, value, subtitle }: KpiCardProps) {
  return (
    <div className="card">
      <h3>{title}</h3>
      <strong>{value}</strong>
      {subtitle ? <span className="badge">{subtitle}</span> : null}
    </div>
  );
}
