<template>
<div class="new-book">
  <BookWizard></BookWizard>
</div>
</template>

<script>
import BookWizard from '@/components/BookWizard.vue';
export default {
  name: 'new-book',
  components: { BookWizard },
  methods: {
  },
  data() {
    return {
      toc: null,
    };
  },
};
</script>

<style lang="scss">
.toc {
  flex-grow: 1;
  overflow-y: scroll;
}

.new-book {
  padding: 2rem;
  background: white;
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
  width: 40vw;
  display: flex;
  flex-direction: column;

  h2 {
    font-weight: 400;
    margin: 0 0 0.5rem;
  }

  &-footer {
    display: flex;
    margin-top: 0.5rem;
    align-items: center;

    * {
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }
  }
}
</style>
