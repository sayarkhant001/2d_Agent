export async function fetchLive2D() {
  const res = await fetch('https://api.thaistock2d.com/live');
  if (!res.ok) throw new Error('Live API HTTP ' + res.status);
  return await res.json();
}

export async function fetch30DayHistory() {
  const res = await fetch('https://api.thaistock2d.com/2d_result');
  if (!res.ok) throw new Error('History API HTTP ' + res.status);
  return await res.json();
}
