import Vue from 'vue';
import Vuex from 'vuex';
import api from '@/api';
import auth from '@/store/auth';
import notifications from '@/store/notifications';

Vue.use(Vuex);

function prepareSeries(path, series, seriesFilter, bookFilter, excluded) {
  series.books.forEach((b) => {
    Vue.set(
      b,
      'selected',
      (excluded === null || !excluded.includes(b.id)) && (
        (seriesFilter === null && bookFilter === null)
        || bookFilter === null
        || (seriesFilter !== null && seriesFilter.some(f => path.match(f)))
        || (bookFilter !== null && bookFilter.includes(b.id))
      ),
    );
  });
  series.children.forEach(s => prepareSeries(
    `${path}\\${s.name}`,
    s,
    seriesFilter,
    bookFilter,
    excluded,
  ));
}

const cachedSeries = window.localStorage.getItem('series');
const cachedTags = window.localStorage.getItem('tags');
let parsedTags = {};
if (cachedTags) {
  parsedTags = JSON.parse(cachedTags);
  const keys = Object.keys(parsedTags);

  if (keys.length > 0 && !parsedTags[keys[0]].default) {
    parsedTags = {};
  }
}
const initialState = {
  darkMode: window.localStorage.getItem('darkMode') === 'true',
  series: !cachedSeries ? [] : JSON.parse(cachedSeries),
  tags: parsedTags,
};

const mutations = {
  setDarkMode(state, enabled) {
    state.darkMode = enabled;
    window.localStorage.setItem('darkMode', enabled ? 'true' : 'false');
  },
  setSeries(state, series) {
    state.series = series;
    window.localStorage.setItem('series', JSON.stringify(series));
  },
  setTags(state, tags) {
    state.tags = tags;
    window.localStorage.setItem('tags', JSON.stringify(tags));
  },
  applySeriesFilter(state, { seriesFilter, bookFilter, excluded }) {
    if (state.series === null) {
      return;
    }
    state.series.forEach(s => prepareSeries(s.name, s, seriesFilter, bookFilter, excluded));
    window.localStorage.setItem('series', JSON.stringify(state.series));
  },
};

const actions = {
  async refreshSeries({ commit }) {
    const { data: series } = await api.get('/books/series');
    commit('setSeries', series);
  },
  async refreshTags({ commit }) {
    const { data: tags } = await api.get('/books/tags');

    const ordered = {};
    Object.keys(tags).sort().forEach((key) => {
      ordered[key] = tags[key];
    });

    commit('setTags', ordered);
  },
  async loadDefaultFilter({ commit }, fallback) {
    try {
      const { data: stored } = await api.get('/auth/default-filter');

      commit('applySeriesFilter', stored);
    } catch (error) {
      if (error.response && error.response.status === 404) {
        commit('applySeriesFilter', fallback);
        return;
      }

      throw error;
    }
  },
};

const getters = {
  optionalBooks(state) {
    const result = [];

    function handleSeries(series) {
      result.push(...series.books.filter(b => b.searchedByDefault !== true).map(b => b.id));

      series.children.forEach(handleSeries);
    }

    state.series.forEach(handleSeries);

    return result;
  },
};

export default new Vuex.Store({
  state: initialState,
  mutations,
  actions,
  getters,
  modules: {
    auth,
    notifications,
  },
});
