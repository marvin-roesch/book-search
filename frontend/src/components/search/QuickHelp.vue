<template functional>
<div class="quick-help">
  <ul>
    <li>Exact phrase: <code>"quote it"</code></li>
    <li>Case sensitive: <code>text.cs:Wit</code></li>
    <li>Match 0+: <code>Kala*</code></li>
    <li>Match 1: <code>Dalina?</code></li>
    <li>Boolean: <code>AND</code> (default), <code>OR</code></li>
    <li>Negation: <code>-knot</code></li>
  </ul>
</div>
</template>

<script>
export default {
  name: 'QuickHelp',
};
</script>

<style lang="scss">
.quick-help {
  box-sizing: border-box;
  flex-grow: 1;
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: var(--muted-text-color);

  code {
    background: rgba(0, 0, 0, 0.05);
    padding: 0.125rem;
  }

  ul {
    list-style-type: none;
    display: flex;
    padding: 0;
    margin: 0;
    flex-wrap: wrap;

    li {
      margin-right: 0.25rem;

      &:after {
        display: inline-block;
        content: '·';
        margin-left: 0.25rem;
      }

      &:last-child {
        margin-right: 0;

        &:after {
          display: none;
        }
      }
    }
  }
}
</style>
