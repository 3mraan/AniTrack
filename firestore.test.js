const { describe, it, before, after, beforeEach } = require('node:test');
const fs = require('node:fs');
const { createRequire } = require('node:module');

const rulesRequire = createRequire('/usr/local/lib/node_modules/@firebase/rules-unit-testing/package.json');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} = rulesRequire('@firebase/rules-unit-testing');
const firebase = rulesRequire('firebase/compat/app');
rulesRequire('firebase/compat/firestore');

const PROJECT_ID = 'gen-lang-client-0643252832';
const DATABASE_ID = 'ai-studio-android-stitchzi-6f1e76ea-78e6-40a9-bcba-d590a6744840';

let testEnv;

function ts() {
  return firebase.firestore.FieldValue.serverTimestamp();
}

function validProfile(uid) {
  return {
    uid,
    displayName: 'Ren Takahashi',
    handle: '@ren',
    bio: 'Anime tracker',
    favoriteGenres: ['Action', 'Fantasy'],
    tutorialCompleted: false,
    createdAt: ts(),
    updatedAt: ts()
  };
}

function validLibraryEntry(animeId) {
  return {
    animeId,
    title: 'Solo Leveling',
    posterUrl: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-it355ZgzquUd.png',
    status: 'WATCHING',
    watchedEpisodes: 3,
    currentSeason: 1,
    inLibrary: true,
    isFavorite: false,
    reminderEnabled: false,
    personalNotes: 'Great animation',
    createdAt: ts(),
    updatedAt: ts()
  };
}

function validReview(animeId) {
  return {
    animeId,
    overallRating: 9.0,
    storyScore: 8,
    animationScore: 10,
    charactersScore: 8,
    soundtrackScore: 9,
    reviewText: 'Awesome',
    containsSpoilers: false,
    createdAt: ts(),
    updatedAt: ts()
  };
}

function validWatchlist(id) {
  return {
    id,
    name: 'Top Action',
    description: 'Favorite action anime',
    coverKey: 'solo_leveling',
    animeIds: ['solo_leveling'],
    createdAt: ts(),
    updatedAt: ts()
  };
}

before(async () => {
  const rules = fs.readFileSync('firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: '127.0.0.1',
      port: 8085
    }
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

describe('AniTrack Firestore Security Rules', () => {
  it('denies unauthenticated access to all user documents and subcollections', async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
    await assertFails(anonDb.collection('users').doc('userA').get());
    await assertFails(anonDb.collection('users').doc('userA').set(validProfile('userA')));
    await assertFails(
      anonDb.collection('users').doc('userA').collection('library').doc('solo_leveling').set(validLibraryEntry('solo_leveling'))
    );
  });

  it('allows owner to create and update their profile while blocking cross-user access and createdAt tampering', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore(DATABASE_ID);
    const bobDb = testEnv.authenticatedContext('bob').firestore(DATABASE_ID);

    const aliceProfileRef = aliceDb.collection('users').doc('alice');
    await assertSucceeds(aliceProfileRef.set(validProfile('alice')));
    await assertSucceeds(aliceProfileRef.get());

    // Bob cannot read or write Alice's profile
    await assertFails(bobDb.collection('users').doc('alice').get());
    await assertFails(bobDb.collection('users').doc('alice').set(validProfile('alice')));

    // Owner update preserving createdAt succeeds
    await assertSucceeds(aliceProfileRef.update({
      tutorialCompleted: true,
      updatedAt: ts()
    }));

    // Owner cannot mutate createdAt
    await assertFails(aliceProfileRef.update({
      createdAt: ts(),
      updatedAt: ts()
    }));

    // Owner cannot exceed displayName limit
    await assertFails(aliceProfileRef.update({
      displayName: 'x'.repeat(101),
      updatedAt: ts()
    }));
  });

  it('enforces schema and ownership on library entries, reviews, and watchlists', async () => {
    const aliceDb = testEnv.authenticatedContext('alice').firestore(DATABASE_ID);
    const bobDb = testEnv.authenticatedContext('bob').firestore(DATABASE_ID);

    const libRef = aliceDb.collection('users').doc('alice').collection('library').doc('solo_leveling');
    await assertSucceeds(libRef.set(validLibraryEntry('solo_leveling')));
    await assertFails(libRef.set({
      ...validLibraryEntry('solo_leveling'),
      status: 'INVALID_STATUS'
    }));
    await assertFails(bobDb.collection('users').doc('alice').collection('library').doc('solo_leveling').get());

    const revRef = aliceDb.collection('users').doc('alice').collection('reviews').doc('solo_leveling');
    await assertSucceeds(revRef.set(validReview('solo_leveling')));
    await assertFails(
      aliceDb.collection('users').doc('alice').collection('reviews').doc('bad').set({
        ...validReview('bad'),
        overallRating: 11
      })
    );

    const wlRef = aliceDb.collection('users').doc('alice').collection('watchlists').doc('wl_1');
    await assertSucceeds(wlRef.set(validWatchlist('wl_1')));
    await assertSucceeds(wlRef.delete());
  });
});
