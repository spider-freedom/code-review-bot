import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DiffViewer from '@/components/DiffViewer.vue'

describe('DiffViewer', () => {
  it('空字符串渲染空状态提示', () => {
    const wrapper = mount(DiffViewer, { props: { diffText: '' } })
    expect(wrapper.find('.diff-empty').exists()).toBe(true)
    expect(wrapper.find('.diff-table').exists()).toBe(false)
  })

  it('正确解析新增行和删除行', () => {
    const diff = [
      '@@ -1,3 +1,3 @@',
      '-const oldVar = 1',
      '+const newVar = 2',
    ].join('\n')

    const wrapper = mount(DiffViewer, { props: { diffText: diff } })

    const rows = wrapper.findAll('.diff-row')
    // hunk + delete + add = 3 行
    expect(rows).toHaveLength(3)

    // Hunk 行
    expect(rows[0].classes()).toContain('diff-hunk')

    // 删除行
    expect(rows[1].classes()).toContain('diff-delete')
    expect(rows[1].find('.line-sign').text()).toBe('-')
    expect(rows[1].find('.line-content').text()).toBe('const oldVar = 1')

    // 新增行
    expect(rows[2].classes()).toContain('diff-add')
    expect(rows[2].find('.line-sign').text()).toBe('+')
    expect(rows[2].find('.line-content').text()).toBe('const newVar = 2')
  })

  it('行号正确递增', () => {
    const diff = [
      '@@ -1,0 +1,3 @@',
      ' func main() {',
      '+  line1',
      '+  line2',
    ].join('\n')

    const wrapper = mount(DiffViewer, { props: { diffText: diff } })
    const rows = wrapper.findAll('.diff-row')

    // Row 0: hunk (no line nums)
    // Row 1: normal (oldLine=1, newLine=1)
    // Row 2: add (oldLine=undefined, newLine=2)
    // Row 3: add (oldLine=undefined, newLine=3)

    expect(rows[1].find('.old-num').text()).toBe('1')
    expect(rows[1].find('.new-num').text()).toBe('1')

    expect(rows[2].find('.old-num').text()).toBe('')
    expect(rows[2].find('.new-num').text()).toBe('2')

    expect(rows[3].find('.new-num').text()).toBe('3')
  })

  it('正确解析 diff header 行', () => {
    const diff = [
      'diff --git a/file.ts b/file.ts',
      'index abc123..def456 100644',
      '--- a/file.ts',
      '+++ b/file.ts',
      '@@ -1,1 +1,1 @@',
      ' unchanged line',
    ].join('\n')

    const wrapper = mount(DiffViewer, { props: { diffText: diff } })
    const rows = wrapper.findAll('.diff-row')

    // 4 header + 1 hunk + 1 normal = 6 行
    expect(rows).toHaveLength(6)
    expect(rows[0].classes()).toContain('diff-header')
    expect(rows[1].classes()).toContain('diff-header')
    expect(rows[2].classes()).toContain('diff-header')
    expect(rows[3].classes()).toContain('diff-header')
    expect(rows[4].classes()).toContain('diff-hunk')
    expect(rows[5].classes()).toContain('diff-normal')
  })

  it('多行 diff 内容完整渲染', () => {
    const diff = `diff --git a/test.ts b/test.ts
--- a/test.ts
+++ b/test.ts
@@ -1,4 +1,4 @@
 const a = 1
-const b = 2
+const b = 3
 const c = 4
-const d = 5
+const d = 6`

    const wrapper = mount(DiffViewer, { props: { diffText: diff } })
    const addRows = wrapper.findAll('.diff-add')
    const delRows = wrapper.findAll('.diff-delete')
    const normalRows = wrapper.findAll('.diff-normal')

    expect(addRows).toHaveLength(2)
    expect(delRows).toHaveLength(2)
    expect(normalRows).toHaveLength(2)
  })
})
