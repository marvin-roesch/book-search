<template>
<div class="new-book">
  <UserPanel></UserPanel>
  <BookWizard></BookWizard>
</div>
</template>

<script>
import UserPanel from '@/components/UserPanel.vue';
import BookWizard from '@/components/management/wizard/BookWizard.vue';

export default {
  name: 'edit-book',
  components: { BookWizard, UserPanel },
};
</script>

<style lang="scss">
.edit-book {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;

  .user-panel {
    position: absolute;
    top: 19.5px;
    left: 32px;
  }
}
</style>
