<template>
<Fullscreen user-panel>
  <UserList></UserList>
</Fullscreen>
</template>

<script>
import UserList from '@/components/management/UserList.vue';
import Fullscreen from '@/views/Fullscreen.vue';

export default {
  name: 'user-management',
  components: { Fullscreen, UserList },
};
</script>
