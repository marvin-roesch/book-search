<template>
<div class="user-management">
  <UserPanel></UserPanel>
  <UserList></UserList>
</div>
</template>

<script>
import UserList from '@/components/management/UserList.vue';
import UserPanel from '@/components/UserPanel.vue';

export default {
  name: 'user-management',
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
    top: 19.5px;
    left: 32px;
  }
}
</style>
