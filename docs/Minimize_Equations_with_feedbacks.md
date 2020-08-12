# Additional minimization of PAL equations by using feedback outputs

## Introduction

Sometimes, truth tables brute-forced from a PAL and minimized with espresso contain expressions that are too long to fit into a newly programmed GAL/PAL device, which has limits in the number of usable terms.
This is because the brute-forcing and minimization do not take into account the possibility to use feedback outputs (outputs the values of which are used as inputs to other equations) included in a PAL device to further reduce the number of necessary terms.
The equations generated by brute-forcing and espresso are equivalent to the originals, but additional simplification making use feedback outputs can be occasionally required. In this case, we can use **Logic Friday** to help us in the procedure.

## The Procedure

The procedure can be summarized by the following steps:

1. Minimize the truth table and obtain the equations.
2. Scan the equations to find ones that appear long and have many terms shared with others.
    - You should also identify equations that are possible feedbacks you can use to simplify the other equations.
3. Extract the equations, put them in **Logic Friday**, minimize and turn them into __Products of Sums__.
4. Massage the equations to find how to replace terms by using the feedbacks.
5. Repeat 2-4 as needed.

### Example

#### Step 1

After going through the automatic analisys and bruteforcing of a *PAL16L8* IC, we end up with a truth table that put through **espresso** minimizes to the following equations:

```text
!io18 = (!i2&i3&!i4&i5&!i8&!io13) | (!i2&i3&!i4&!i6&i8&!io13) | (!i2&i3&!i4&!i5&i6&!io13) | (!i2&i3&!i4&!i7&!io13) | (i1&!i2&i3&!i4&!io13);

!io17 = (i2&!i3&i4&i5&!i8&!io13) | (i2&!i3&i4&!i6&i8&!io13) | (i2&!i3&i4&!i5&i6&!io13) | (i2&!i3&i4&!i7&!io13) | (i1&i2&!i3&i4&!io13);

!io16 = (i2&!i3&!i4&i5&!i8&!io13) | (i2&!i3&!i4&!i6&i8&!io13) | (i2&!i3&!i4&!i5&i6&!io13) | (i2&!i3&!i4&!i7&!io13) | (i1&i2&!i3&!i4&!io13);

!io15 = (!i1&i5&i6&i7&i8&!io13);

!io14 = (!i7&!io13) | (i1&!io13) | (i8&!io13) | (i6&!io13) | (i5&!io13);

!o19 = (!i2&!i3&!i4&i5&!i8&!io13) | (!i2&!i3&!i4&!i6&i8&!io13) | (!i2&!i3&!i4&!i5&i6&!io13) | (!i2&!i3&!i4&!i7&!io13) | (i1&!i2&!i3&!i4&!io13);

!o12 = (!i1&!i5&!i6&i7&!i8&!i9&!i11);
```

Notice that **espresso** will NOT put the exclamation mark at the beginning of every equation. But as the PAL16L8 is an active-low device, and the minimized equations are representative of the OFF-set of the truth table, I added it myself for clarity.

#### Step 2

Notice how many of the equations that we obtained in the previous step are sharing a lot of similar terms with `!io14` and `!io15`.
We'll randomly pick `!io16` to simplify in the following steps.

#### Step 3

Let's fire up **Logic Friday** and put the equations in.

Below you can see the equation for `!io16`, minimized and converted into a **Product of Sums** (let’s replace the negation at the beginning of the equation with an `n` for the sake of simplicity and because Logic Friday would error out with such syntax: we’ll just add it back when preparing the equations to program on a new chip).

```text
Entered:
nio16 = (i2&!i3&!i4&i5&!i8&!io13) | (i2&!i3&!i4&!i6&i8&!io13) | (i2&!i3&!i4&!i5&i6&!io13) | (i2&!i3&!i4&!i7&!io13) | (i1&i2&!i3&!i4&!io13);

Minimized:
nio16 = i2 i3' i4' io13' i7'  + i2 i3' i4' io13' i1 + i2 i3' i4' i5' i8 io13'  + i2 i3' i4' i8' io13' i6  + i2 i3' i4' i5 io13' i6' ;

Minimized Product of Sums:
nio16 = (io13')(i4')(i3')(i2)(i5'+i8'+i6'+i7'+i1)(i5+i8+i6+i7'+i1);
```

Then we do the same for the equation of `!io14`:

```text
Entered:
nio14 = (!i7&!io13) | (i1&!io13) | (i8&!io13) | (i6&!io13) | (i5&!io13);

Minimized:
nio14 = i7' io13'  + io13' i1  + io13' i8  + io13' i6  + io13' i5;

Minimized Product of Sums:
nio14 = (io13')(i7'+i1+i8+i6+i5);
```

Begin to notice similarities?

Let's do the same for `!io15`:

```text
Entered:
nio15 = (!i1&i5&i6&i7&i8&!io13);

Minimized:
nio15 = i1' i5 i6 i7 i8 io13';
```

We don't need to turn this into a **Product of Sums** as we have no sums here.

#### Step 4

We can see that all of `!io14` appears in the equation of `!io16`. And we can also notice that a term very similar
to `!io15` is also in `!io16`.

First, we simply replace `!io14` in `!io16` and get:

```text
nio16 = (io14')(i4')(i3')(i2)(i5'+i8'+i6'+i7'+i1);
```

Note how the term `io14` is negated in the equation : remember what You read above, that every equation is of the OFF-set of the truth table and that we’ve replaced the negation with an `n` for simplicity when working with **Logic Friday**. Now we just added it back.

Then, let’s look at `!io15` and what happens if we invert it:

```text
Entered:
io15 = !(!i1&i5&i6&i7&i8&!io13);

Minimized:
io15 = i1  + i5'  + i6'  + i7'  + i8'  + io13;
```

This time it's no longer `nio15`, because, having inverted it, it now represents the ON-set of the truth table, and as such it's just `io15`.

So, looking at `!io16`, we see there is a group of terms almost identical to `io15` (`(i5'+i8'+i6'+i7'+i1)`), but lacking `io13`, so we’ll simply have to add it back in negated form to counter the one we’re adding by replacing the group with `io15`:

```text
nio16 = (io14')(i4')(i3')(i2)(i5'+i8'+i6'+i7'+i1);
```

becomes:

```text
nio16 = (io14')(i4')(i3')(i2)(io15 & io13');
```

Notice that in this case `io15` is not negated: we already inverted it, remember?

We can now minimize `!io16` again and see what we get:

```text
Entered:
io16 = (io14')(i4')(i3')(i2)(io15 & io13');

Minimized:
io16 = i4' i3' i2 io15 io13' io14';

```

##### Note

So, You might be wondering
> why you replaced a term with `io13` in OR and added `io13’` in AND?”

Just think about it this way: you had a condition inside the parentheses, that condition was not dependent on the status of `io13` (it was `i1 | !i5 | !i6 | !i7 | !i8`), but we replaced it with a condition that depended on `io13` (`io15` included that term in OR, so it is sufficient for it to be **true** to turn **true** the whole term between parentheses).

To counter it, we need to make sure that, if the condition is verified, it is so **without** `io13` being **true**, and we can do it by adding an AND condition that checks for this.

It’s perhaps clearer by looking at the expanded equation

```text
nio16 = (io14')(i4')(i3')(i2)(io15 & io13');
nio16 = (io14')(i4')(i3')(i2)((i1 + i5' + i6' + i7' + i8' + io13) & io13');
```

## Links & References

- A modern and compilable re-host of the [Espresso](https://github.com/classabbyamp/espresso-logic) heuristic logic minimizer.
- [Logic Friday](https://download.cnet.com/developer/logic-friday/i-10268041) 1.1.4