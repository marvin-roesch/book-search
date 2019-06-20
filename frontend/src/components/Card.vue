<template>
<component :is="tag" class="card">
  <h2><slot name="title">{{ title }}</slot></h2>
  <slot></slot>
  <div class="card-footer" v-if="$slots.footer">
    <slot name="footer"></slot>
  </div>
</component>
</template>

<script>
export default {
  name: 'Card',
  props: {
    tag: {
      type: String,
      default: 'div',
    },
    title: String,
  },
};
</script>

<style lang="scss">
.card {
  padding: 2rem;
  background: white;
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);

  h2 {
    font-weight: 400;
    margin: 0 0 0.5rem;
  }

  &-footer {
    display: flex;
    margin-top: 0.5rem;
    align-items: center;
    justify-content: flex-end;

    * {
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
        margin-left: auto;
      }
    }
  }
}
</style>
