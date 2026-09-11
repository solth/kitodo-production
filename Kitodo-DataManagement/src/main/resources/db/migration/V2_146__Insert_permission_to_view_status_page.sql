--
-- (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
--
-- This file is part of the Kitodo project.
--
-- It is licensed under GNU General Public License version 3 or later.
--
-- For the full copyright and license information, please read the
-- GPL3-License.txt file that was distributed with this source code.
--

-- 1. Add permission to view the status page

INSERT IGNORE INTO authority (title) VALUES ('viewStatusPage_globalAssignable');

INSERT IGNORE INTO role_x_authority (role_id, authority_id)
SELECT (SELECT id FROM role WHERE title = 'Administration'), id FROM authority WHERE title = 'viewStatusPage_globalAssignable';
