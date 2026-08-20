export type AgentId = 'problem-convert' | 'testdata-gen'

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
  {
    id: 'testdata-gen',
    name: '数据生成智能体',
    description: '生成 Python 脚本，本机运行产出 1.in/1.out … N.in/N.out 成对测试数据',
    badge: 'OI',
  },
]

export function findAgent(id: string | null | undefined): AgentDefinition | undefined {
  if (!id) return undefined
  return AGENT_LIST.find((item) => item.id === id)
}
