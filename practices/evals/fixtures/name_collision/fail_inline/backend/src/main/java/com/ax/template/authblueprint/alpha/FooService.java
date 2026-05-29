package com.ax.template.authblueprint.alpha;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// Regression: inline @Service @Transactional on one line — pre-fix this ESCAPED the
// own-line anchor `@Service\s*(\(|$)`, so the cross-package FooService collision was missed.
@Service @Transactional
public class FooService {}
