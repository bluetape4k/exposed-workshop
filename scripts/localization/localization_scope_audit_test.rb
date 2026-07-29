#!/usr/bin/env ruby
# frozen_string_literal: true

require "open3"
require "minitest/autorun"

ROOT = File.expand_path("../..", __dir__)
AUDIT = File.join(ROOT, "scripts/localization/audit_scope.rb")

class LocalizationScopeAuditTest < Minitest::Test
  def run_audit(*args)
    Open3.capture3("ruby", AUDIT, *args)
  end

  def test_strict_inventory_matches_approved_baseline
    stdout, stderr, status = run_audit("--strict-counts")

    assert status.success?, stderr
    assert_includes stdout, "single_language_docs=104"
    assert_includes stdout, "readme_pair_dirs=86"
    assert_includes stdout, "comment_bearing_kotlin_files=604"
    assert_includes stdout, "kdoc_tag_files=102"
  end

  def test_current_branch_does_not_touch_primary_scope_exclusions
    stdout, stderr, status = run_audit("--base", "develop", "--check-diff")

    assert status.success?, stderr
    assert_includes stdout, "Localization scope inventory"
  end
end
