import Vue from 'vue';
import Vuex from 'vuex';
import api from '@/api';
import auth from '@/store/auth';
import notifications from '@/store/notifications';

Vue.use(Vuex);

function prepareSeries(path, series, seriesFilter, bookFilter) {
  series.books.forEach((b) => {
    Vue.set(
      b,
      'selected',
      (seriesFilter === null && bookFilter === null && b.searchedByDefault)
      || (bookFilter === null && b.searchedByDefault)
      || (b.searchedByDefault && seriesFilter.some(f => path.match(f)))
      || (bookFilter !== null && bookFilter.includes(b.id)),
    );
  });
  series.children.forEach(s => prepareSeries(`${path}\\${s.name}`, s, seriesFilter, bookFilter));
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
  applySeriesFilter(state, { seriesFilter, bookFilter }) {
    if (state.series === null) {
      return;
    }
    state.series.forEach(s => prepareSeries(s.name, s, seriesFilter, bookFilter));
    window.localStorage.setItem('series', JSON.stringify(state.series));
  },
};

const actions = {
  async refreshSeries({ commit }, filter) {
    const { data: series } = await api.get('/books/series');
    commit('setSeries', series);
    commit('applySeriesFilter', filter);
  },
  async refreshTags({ commit }) {
    const { data: tags } = await api.get('/books/tags');

    const ordered = {};
    Object.keys(tags).sort().forEach((key) => {
      ordered[key] = tags[key];
    });

    commit('setTags', ordered);
  },
};

export default new Vuex.Store({
  state: initialState,
  mutations,
  actions,
  modules: {
    auth,
    notifications,
  },
});
