<template>
<div class="book-management">
  <UserPanel></UserPanel>
  <BookList></BookList>
</div>
</template>

<script>
import UserPanel from '@/components/UserPanel.vue';
import BookList from '@/components/management/BookList.vue';

export default {
  name: 'book-management',
  components: { BookList, UserPanel },
};
</script>

<style lang="scss">
.book-management {
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
