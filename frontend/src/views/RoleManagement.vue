<template>
<Fullscreen user-panel>
  <RoleList></RoleList>
</Fullscreen>
</template>

<script>
import RoleList from '@/components/management/RoleList.vue';
import Fullscreen from '@/views/Fullscreen.vue';

export default {
  name: 'role-management',
  components: { Fullscreen, RoleList },
};
</script>
