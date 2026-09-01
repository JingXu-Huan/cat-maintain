const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`)

  if (!response.ok) {
    throw new Error(`接口请求失败（HTTP ${response.status}）`)
  }

  return response.json() as Promise<T>
}
