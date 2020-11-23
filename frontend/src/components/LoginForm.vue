<template>
<Form class="login-form" title="Login">
  <TextField
    name="username"
    v-model="username"
    placeholder="Username"
  >
    <template slot="icon">
    <UserIcon></UserIcon>
    </template>
  </TextField>
  <TextField
    type="password"
    name="password"
    v-model="password"
    placeholder="Password"
  >
    <template slot="icon">
    <LockIcon></LockIcon>
    </template>
  </TextField>
  <template slot="footer">
  <Button slim @click="login" :loading="verifying">Login</Button>
  </template>
</Form>
</template>

<script>
import { LockIcon, UserIcon } from 'vue-feather-icons';
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';
import Form from '@/components/Form.vue';

export default {
  name: 'LoginForm',
  components: {
    Form, Button, TextField, LockIcon, UserIcon,
  },
  data() {
    return {
      username: '',
      password: '',
      verifying: false,
    };
  },
  methods: {
    async login() {
      const {
        username, password,
      } = this;
      this.verifying = true;
      try {
        if (await this.$store.dispatch('auth/login', { username, password })) {
          this.$emit('first-login');
        } else {
          this.$router.replace(this.$route.query.redirect || '/');
        }
      } catch (error) {
        this.$handleApiError(error);
      }
      this.verifying = false;
    },
  },
};
</script>

<style lang="scss">
.login-form {
  width: 35vw;
  max-width: $max-content-width * 0.7;
  box-sizing: border-box;
  margin: 0 auto;

  @media (max-width: $max-content-width) {
    width: 50vw;
  }

  @media (max-width: 640px) {
    width: 100%;
  }
}
</style>
