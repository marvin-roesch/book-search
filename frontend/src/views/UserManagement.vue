<template>
<div class="user-management">
  <UserPanel></UserPanel>
  <UserList></UserList>
</div>
</template>

<script>
import UserList from '@/components/management/users/UserList.vue';
import UserPanel from '@/components/UserPanel.vue';

export default {
  name: 'new-book',
  components: { UserPanel, UserList },
};
</script>

<style lang="scss">
.user-management {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;

  .user-panel {
    position: absolute;
    top: 1.35rem;
    right: 2rem;
  }
}
</style>
