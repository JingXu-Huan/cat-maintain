export async function loadAllPages<T>(load: (page: number, size: number) => Promise<{ content: T[]; total: number }>): Promise<T[]> {
  const result: T[] = []
  for (let page = 0; ; page++) {
    const response = await load(page, 100)
    result.push(...response.content)
    if (response.content.length === 0 || result.length >= response.total) return result
  }
}
