<template>
<Fullscreen user-panel>
  <AccountSettings></AccountSettings>
</Fullscreen>
</template>

<script>
import AccountSettings from '@/components/AccountSettings.vue';
import Fullscreen from '@/views/Fullscreen.vue';

export default {
  name: 'user-profile',
  components: { Fullscreen, AccountSettings },
};
</script>
