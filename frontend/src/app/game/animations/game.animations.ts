import {
  trigger,
  transition,
  style,
  animate,
  keyframes,
  query,
  stagger,
  state,
} from '@angular/animations';

/** Card entering a zone (hand, battlefield) */
export const cardEnter = trigger('cardEnter', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(0.6) translateY(30px)' }),
    animate('300ms cubic-bezier(0.35, 0, 0.25, 1)',
      style({ opacity: 1, transform: 'scale(1) translateY(0)' })
    ),
  ]),
  transition(':leave', [
    animate('200ms ease-in',
      style({ opacity: 0, transform: 'scale(0.8) translateY(-20px)' })
    ),
  ]),
]);

/** Card being played from hand */
export const cardPlay = trigger('cardPlay', [
  transition(':leave', [
    animate('400ms cubic-bezier(0.4, 0, 0.2, 1)', keyframes([
      style({ transform: 'scale(1) translateY(0)', opacity: 1, offset: 0 }),
      style({ transform: 'scale(1.15) translateY(-40px)', opacity: 0.9, offset: 0.4 }),
      style({ transform: 'scale(0.5) translateY(-80px)', opacity: 0, offset: 1 }),
    ])),
  ]),
]);

/** Creature summoned to battlefield */
export const creatureSummon = trigger('creatureSummon', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(0) rotateY(90deg)' }),
    animate('500ms cubic-bezier(0.35, 0, 0.25, 1)', keyframes([
      style({ opacity: 0, transform: 'scale(0) rotateY(90deg)', offset: 0 }),
      style({ opacity: 0.7, transform: 'scale(1.1) rotateY(10deg)', offset: 0.6 }),
      style({ opacity: 1, transform: 'scale(1) rotateY(0)', offset: 1 }),
    ])),
  ]),
  transition(':leave', [
    animate('300ms ease-in', keyframes([
      style({ opacity: 1, transform: 'scale(1)', offset: 0 }),
      style({ opacity: 0.5, transform: 'scale(1.1)', offset: 0.3 }),
      style({ opacity: 0, transform: 'scale(0) rotateZ(10deg)', offset: 1 }),
    ])),
  ]),
]);

/** Damage flash effect (applied via class toggle) */
export const damageFlash = trigger('damageFlash', [
  state('idle', style({})),
  state('hit', style({})),
  transition('idle => hit', [
    animate('400ms ease-out', keyframes([
      style({ filter: 'brightness(1)', offset: 0 }),
      style({ filter: 'brightness(2.5) saturate(0.3)', offset: 0.15 }),
      style({ filter: 'brightness(0.6) saturate(1.5)', offset: 0.4 }),
      style({ filter: 'brightness(1)', offset: 1 }),
    ])),
  ]),
]);

/** Heal glow effect */
export const healGlow = trigger('healGlow', [
  state('idle', style({})),
  state('healed', style({})),
  transition('idle => healed', [
    animate('600ms ease-out', keyframes([
      style({ boxShadow: '0 0 0 rgba(114, 184, 148, 0)', offset: 0 }),
      style({ boxShadow: '0 0 20px rgba(114, 184, 148, 0.8)', offset: 0.3 }),
      style({ boxShadow: '0 0 40px rgba(114, 184, 148, 0.4)', offset: 0.6 }),
      style({ boxShadow: '0 0 0 rgba(114, 184, 148, 0)', offset: 1 }),
    ])),
  ]),
]);

/** Freeze effect */
export const freezeEffect = trigger('freezeEffect', [
  state('idle', style({})),
  state('frozen', style({ filter: 'hue-rotate(180deg) brightness(1.2)' })),
  transition('idle => frozen', [
    animate('500ms ease-out'),
  ]),
  transition('frozen => idle', [
    animate('300ms ease-in'),
  ]),
]);

/** Event log item entering */
export const logEntry = trigger('logEntry', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateX(-20px)', maxHeight: '0' }),
    animate('250ms ease-out',
      style({ opacity: 1, transform: 'translateX(0)', maxHeight: '60px' })
    ),
  ]),
]);

/** Stagger children entering */
export const listStagger = trigger('listStagger', [
  transition('* => *', [
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(10px)' }),
      stagger(50, [
        animate('200ms ease-out', style({ opacity: 1, transform: 'translateY(0)' })),
      ]),
    ], { optional: true }),
  ]),
]);

/** Mana crystal fill */
export const manaFill = trigger('manaFill', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(0)' }),
    animate('200ms 50ms ease-out',
      style({ opacity: 1, transform: 'scale(1)' })
    ),
  ]),
]);

/** Pulse for turn indicator */
export const turnPulse = trigger('turnPulse', [
  state('active', style({})),
  state('waiting', style({ opacity: 0.6 })),
  transition('waiting => active', [
    animate('500ms ease-out', keyframes([
      style({ transform: 'scale(1)', boxShadow: '0 0 0 rgba(217,163,90,0)', offset: 0 }),
      style({ transform: 'scale(1.05)', boxShadow: '0 0 20px rgba(217,163,90,0.6)', offset: 0.5 }),
      style({ transform: 'scale(1)', boxShadow: '0 0 0 rgba(217,163,90,0)', offset: 1 }),
    ])),
  ]),
  transition('active => waiting', [
    animate('300ms ease-in'),
  ]),
]);
