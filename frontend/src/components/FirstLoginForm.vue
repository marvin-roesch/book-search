<template>
<Form class="first-login-form" title="Change password">
  This appears to be your first login.
  It is recommended that you change your password from the default one.
  <TextField
    type="password"
    name="old-password"
    v-model="oldPassword"
    placeholder="Current password"
  >
    <template slot="icon">
    <LockIcon></LockIcon>
    </template>
  </TextField>
  <TextField
    type="password"
    name="new-password"
    v-model="newPassword"
    placeholder="New password"
  >
    <template slot="icon">
    <KeyIcon></KeyIcon>
    </template>
  </TextField>
  <TextField
    type="password"
    name="repeat-password"
    v-model="newPasswordRepeat"
    placeholder="Repeat password"
  >
    <template slot="icon">
    <RepeatIcon></RepeatIcon>
    </template>
  </TextField>
  <template slot="footer">
  <router-link :to="$route.query.redirect || '/'">Don't change</router-link>
  <Button slim @click="change" :loading="verifying">Change</Button>
  </template>
</Form>
</template>

<script>
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';
import Form from '@/components/Form.vue';
import { LockIcon, KeyIcon, RepeatIcon } from 'vue-feather-icons';

export default {
  name: 'FirstLoginForm',
  components: { Form, Button, TextField, LockIcon, KeyIcon, RepeatIcon },
  data() {
    return {
      oldPassword: '',
      newPassword: '',
      newPasswordRepeat: '',
      verifying: false,
    };
  },
  methods: {
    async change() {
      const {
        oldPassword, newPassword, newPasswordRepeat,
      } = this;
      this.verifying = true;
      try {
        const { data: { message } } = await this.$api.patch(
          '/auth/password',
          { oldPassword, newPassword, newPasswordRepeat },
        );
        this.$notifications.success(message);
        this.$router.replace(this.$route.query.redirect || '/');
      } catch (error) {
        this.$handleApiError(error);
      }
      this.verifying = false;
    },
  },
};
</script>

<style lang="scss">
.first-login-form {
  max-width: 20vw;
}
</style>
