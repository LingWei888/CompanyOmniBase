export type AgentId = 'problem-convert'

export interface AgentDefinition {
  id: AgentId
  name: string
  description: string
  badge?: string
}

export const AGENT_LIST: AgentDefinition[] = [
  {
    id: 'problem-convert',
    name: '题意修改智能体',
    description: '按目标标题换皮改写题面，保留原题输入输出、样例与数据范围',
    badge: 'OI',
  },
]

export function findAgent(id: string | null | undefined): AgentDefinition | undefined {
  if (!id) return undefined
  return AGENT_LIST.find((item) => item.id === id)
}
