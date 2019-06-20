<template>
<div class="user-profile">
  <UserPanel></UserPanel>
  <AccountSettings></AccountSettings>
</div>
</template>

<script>
import UserList from '@/components/management/UserList.vue';
import UserPanel from '@/components/UserPanel.vue';
import AccountSettings from '@/components/AccountSettings.vue';

export default {
  name: 'user-profile',
  components: { AccountSettings, UserPanel },
};
</script>

<style lang="scss">
.user-profile {
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
